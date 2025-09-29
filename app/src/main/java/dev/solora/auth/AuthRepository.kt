package dev.solora.auth

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dev.solora.data.UserInfo
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
    private val KEY_SURNAME = stringPreferencesKey("surname")
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
                prefs[KEY_SURNAME] = "" // Initialize surname for consistency
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, surname: String, email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Registration failed: No user returned")
            
            // Update profile with display name
            val fullName = "$name $surname"
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            user.updateProfile(profileUpdates).await()
            
            // Store user info in Firestore
            val userDoc = hashMapOf(
                "name" to name,
                "surname" to surname,
                "email" to email,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("users").document(user.uid).set(userDoc).await()
            
            // Store user info locally
            context.dataStore.edit { prefs ->
                prefs[KEY_USER_ID] = user.uid
                prefs[KEY_EMAIL] = email
                prefs[KEY_NAME] = name
                prefs[KEY_SURNAME] = surname
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

    suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            Log.d("AuthRepository", "Starting Google authentication with Firebase...")
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google authentication failed")

            Log.d("AuthRepository", "Google authentication successful: ${user.email}")

            // Store user info locally
            context.dataStore.edit { prefs ->
                prefs[KEY_USER_ID] = user.uid
                prefs[KEY_NAME] = user.displayName ?: ""
                prefs[KEY_SURNAME] = "" // No surname from Google
                prefs[KEY_EMAIL] = user.email ?: ""
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google authentication failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun registerWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            Log.d("AuthRepository", "Starting Google registration with Firebase...")
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google registration failed")

            Log.d("AuthRepository", "Google registration successful: ${user.email}")
            
            // Parse Google display name into first and last name
            val fullName = user.displayName ?: ""
            val nameParts = fullName.trim().split(" ")
            val firstName = nameParts.getOrNull(0) ?: ""
            val lastName = if (nameParts.size > 1) nameParts.subList(1, nameParts.size).joinToString(" ") else ""

            // Save user to Firestore users collection
            val userDoc = hashMapOf(
                "name" to firstName,
                "surname" to lastName,
                "email" to (user.email ?: ""),
                "provider" to "google",
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("users").document(user.uid).set(userDoc).await()
            Log.d("AuthRepository", "User saved to Firestore users collection")

            // Store user info locally
            context.dataStore.edit { prefs ->
                prefs[KEY_USER_ID] = user.uid
                prefs[KEY_NAME] = firstName
                prefs[KEY_SURNAME] = lastName
                prefs[KEY_EMAIL] = user.email ?: ""
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google registration failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun getCurrentUserInfo(): Flow<UserInfo?> {
        return context.dataStore.data.catch { e -> 
            if (e is IOException) emit(emptyPreferences()) else throw e 
        }.map { prefs ->
            val userId = prefs[KEY_USER_ID]
            val name = prefs[KEY_NAME]
            val surname = prefs[KEY_SURNAME]
            val email = prefs[KEY_EMAIL]
            
            if (!userId.isNullOrEmpty() && !name.isNullOrEmpty() && !surname.isNullOrEmpty() && !email.isNullOrEmpty()) {
                UserInfo(userId, name, surname, email)
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



