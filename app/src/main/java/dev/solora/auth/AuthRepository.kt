package dev.solora.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dev.solora.data.FirebaseUser as UserInfo
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
    private val KEY_HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")

    val currentUser: FirebaseUser? get() = firebaseAuth.currentUser
    
    // Simple check: has the user ever seen the onboarding screen?
    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }.map { prefs ->
        prefs[KEY_HAS_SEEN_ONBOARDING] ?: false
    }
    
    // Mark that onboarding has been seen
    suspend fun markOnboardingComplete() {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAS_SEEN_ONBOARDING] = true
        }
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

    suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            // Sign in with Google credential
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google login failed")

            // Only store info locally, do NOT write to Firestore
            context.dataStore.edit { prefs ->
                prefs[KEY_USER_ID] = user.uid
                prefs[KEY_NAME] = user.displayName ?: ""
                prefs[KEY_EMAIL] = user.email ?: ""
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
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
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
                prefs[KEY_NAME] = name
                prefs[KEY_SURNAME] = surname
                prefs[KEY_EMAIL] = email
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google sign-in failed")

            val fullName = user.displayName ?: ""
            val nameParts = fullName.trim().split(" ")
            val firstName = nameParts.getOrNull(0) ?: ""
            val lastName = if (nameParts.size > 1) nameParts.subList(1, nameParts.size).joinToString(" ") else ""

            val userDoc = hashMapOf(
                "name" to firstName,
                "surname" to lastName,
                "email" to (user.email ?: "")
            )

            firestore.collection("users").document(user.uid).set(userDoc).await()

            // Store user info locally
            context.dataStore.edit { prefs ->
                prefs[KEY_USER_ID] = user.uid
                prefs[KEY_NAME] = user.displayName ?: ""
                prefs[KEY_SURNAME] = "" // no surname from Google
                prefs[KEY_EMAIL] = user.email ?: ""
            }

            Result.success(user)
        } catch (e: Exception) {
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
    
    suspend fun logout(): Result<Unit> {
        return try {
            // Clear local data store but preserve HAS_SEEN_ONBOARDING flag
            context.dataStore.edit { prefs ->
                val hasSeenOnboarding = prefs[KEY_HAS_SEEN_ONBOARDING] ?: false
                prefs.clear()
                // Keep the onboarding flag so returning users see login page, not onboarding
                prefs[KEY_HAS_SEEN_ONBOARDING] = hasSeenOnboarding
            }
            
            // Sign out from Firebase
            firebaseAuth.signOut()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}