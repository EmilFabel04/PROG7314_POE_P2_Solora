package dev.solora.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "auth")

class AuthRepository(private val context: Context) {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_NAME = stringPreferencesKey("name")
    private val KEY_EMAIL = stringPreferencesKey("email")

    val currentUser: FirebaseUser? get() = firebaseAuth.currentUser
    
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.catch { e -> 
        if (e is IOException) emit(emptyPreferences()) else throw e 
    }.map { prefs ->
        val userId = prefs[KEY_USER_ID]
        !userId.isNullOrEmpty() && firebaseAuth.currentUser != null
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Login failed: No user returned")
            
            // Store user info locally
            context.dataStore.edit { prefs ->
                prefs[KEY_USER_ID] = user.uid
                prefs[KEY_EMAIL] = user.email ?: email
                prefs[KEY_NAME] = user.displayName ?: email.substringBefore('@')
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Registration failed: No user returned")
            
            // Update profile with display name
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()
            
            // Store user info in Firestore
            val userDoc = hashMapOf(
                "name" to name,
                "email" to email,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("users").document(user.uid).set(userDoc).await()
            
            // Store user info locally
            context.dataStore.edit { prefs ->
                prefs[KEY_USER_ID] = user.uid
                prefs[KEY_EMAIL] = email
                prefs[KEY_NAME] = name
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            context.dataStore.edit { prefs ->
                prefs.remove(KEY_USER_ID)
                prefs.remove(KEY_EMAIL)
                prefs.remove(KEY_NAME)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<com.google.firebase.auth.FirebaseUser> {
        return try {
            Log.d("AuthRepository", "Starting Google authentication with ID token")
            
            if (idToken.isBlank()) {
                throw Exception("ID token is empty")
            }
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            Log.d("AuthRepository", "Created Google credential successfully")
            
            val result = firebaseAuth.signInWithCredential(credential).await()
            Log.d("AuthRepository", "Firebase signInWithCredential completed")
            
            val user = result.user ?: return Result.failure(Exception("Firebase returned null user"))
            Log.d("AuthRepository", "Google authentication successful for user: ${user.email}")
            
            // Check if this is a new user (for logging purposes)
            val isNewUser = result.additionalUserInfo?.isNewUser ?: false
            Log.d("AuthRepository", "Is new user: $isNewUser")
            
            // Always ensure user document exists in Firestore users collection
            val userDoc = hashMapOf(
                "uid" to user.uid,
                "name" to (user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "provider" to "google",
                "isNewUser" to isNewUser,
                "lastSignIn" to com.google.firebase.Timestamp.now(),
                "createdAt" to if (isNewUser) com.google.firebase.Timestamp.now() else null
            )
            
            // Use merge: true to update existing or create new
            firestore.collection("users").document(user.uid)
                .set(userDoc, SetOptions.merge())
                .await()
            Log.d("AuthRepository", "User document saved to Firestore users collection")
            
            // Store user info locally
            context.dataStore.edit { prefs ->
                prefs[KEY_USER_ID] = user.uid
                prefs[KEY_EMAIL] = user.email ?: ""
                prefs[KEY_NAME] = user.displayName ?: ""
            }
            Log.d("AuthRepository", "User data stored locally")
            
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google authentication failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun registerWithGoogle(idToken: String): Result<com.google.firebase.auth.FirebaseUser> {
        // Google Sign-In handles both login and registration automatically
        // Just delegate to loginWithGoogle since Firebase handles this seamlessly
        Log.d("AuthRepository", "Google registerWithGoogle -> delegating to loginWithGoogle")
        return loginWithGoogle(idToken)
    }

    fun getCurrentUserInfo(): Flow<UserInfo?> {
        return context.dataStore.data.catch { e -> 
            if (e is IOException) emit(emptyPreferences()) else throw e 
        }.map { prefs ->
            val userId = prefs[KEY_USER_ID]
            val name = prefs[KEY_NAME]
            val email = prefs[KEY_EMAIL]
            
            if (!userId.isNullOrEmpty() && !name.isNullOrEmpty() && !email.isNullOrEmpty()) {
                UserInfo(userId, name, email)
            } else null
        }
    }

    suspend fun getFirebaseIdToken(): String? {
        return try {
            firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }
    
    // Test method to verify user exists in Firestore
    suspend fun verifyUserInFirestore(): Result<Map<String, Any>?> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("No authenticated user"))
            }
            
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            if (userDoc.exists()) {
                Log.d("AuthRepository", "User found in Firestore: ${userDoc.data}")
                Result.success(userDoc.data)
            } else {
                Log.w("AuthRepository", "User not found in Firestore")
                Result.failure(Exception("User document not found in Firestore"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error verifying user in Firestore: ${e.message}")
            Result.failure(e)
        }
    }
}

data class UserInfo(
    val id: String,
    val name: String,
    val email: String
)


