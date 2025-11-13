package dev.solora.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dev.solora.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

private val Context.motivationalDataStore by preferencesDataStore(name = "motivational_notifications")

class MotivationalNotificationManager(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("motivational_enabled")
        private val KEY_LAST_NOTIFICATION = longPreferencesKey("last_notification_time")
        private const val NOTIFICATION_COOLDOWN = 24 * 60 * 60 * 1000L // 24 hours
    }

    suspend fun enableMotivationalNotifications(enabled: Boolean) {
        context.motivationalDataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
        
        if (enabled) {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                saveFCMToken(token)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    suspend fun isNotificationsEnabled(): Boolean {
        return context.motivationalDataStore.data.first()[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun checkAndSendMotivationalMessage() {
        if (!isNotificationsEnabled()) return
        if (!canSendNotification()) return
        
        val userId = auth.currentUser?.uid ?: return
        
        try {
            val quotesSnapshot = firestore.collection("quotes")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val quoteCount = quotesSnapshot.size()
            
            val leadsSnapshot = firestore.collection("leads")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val leadCount = leadsSnapshot.size()
            
            val message = generateMotivationalMessage(quoteCount, leadCount)
            
            if (message != null) {
                showLocalNotification(message.first, message.second)
                updateLastNotificationTime()
            }
            
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    private suspend fun canSendNotification(): Boolean {
        val lastNotification = context.motivationalDataStore.data.first()[KEY_LAST_NOTIFICATION] ?: 0L
        return System.currentTimeMillis() - lastNotification > NOTIFICATION_COOLDOWN
    }

    private suspend fun updateLastNotificationTime() {
        context.motivationalDataStore.edit { prefs ->
            prefs[KEY_LAST_NOTIFICATION] = System.currentTimeMillis()
        }
    }

    private fun generateMotivationalMessage(quoteCount: Int, leadCount: Int): Pair<String, String>? {
        return when {
            quoteCount == 0 && leadCount == 0 -> {
                "Welcome to Solora!" to "Ready to create your first quote? Every expert was once a beginner!"
            }
            quoteCount == 1 -> {
                "Congratulations! " to "You've created your first quote! You're on your way to solar success!"
            }
            quoteCount in 2..4 -> {
                "Great progress! " to "You have $quoteCount quotes created. Keep up the excellent work!"
            }
            quoteCount in 5..9 -> {
                "You're on fire! " to "Wow! $quoteCount quotes completed. You're becoming a solar expert!"
            }
            quoteCount in 10..19 -> {
                "Amazing work! " to "You've reached $quoteCount quotes! Your clients are lucky to have you!"
            }
            quoteCount >= 20 -> {
                "Solar Champion! " to "Incredible! $quoteCount quotes created. You're a true solar sales professional!"
            }
            leadCount >= 5 && quoteCount < 5 -> {
                "Time to convert! " to "You have $leadCount leads waiting. Time to turn them into quotes!"
            }
            else -> null
        }
    }

    private fun showLocalNotification(title: String, body: String) {
        val notification = NotificationCompat.Builder(context, "solora_motivational")
            .setSmallIcon(R.drawable.solora_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private suspend fun saveFCMToken(token: String) {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            firestore.collection("users").document(userId)
                .update("fcmToken", token)
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    private fun getRandomMotivationalQuote(): Pair<String, String> {
        val quotes = listOf(
            "Keep Shining! " to "Every solar panel you sell helps save the planet!",
            "Solar Success! " to "You're not just selling solar, you're selling a brighter future!",
            "Energy Expert! " to "Your dedication to renewable energy is inspiring!",
            "Green Champion! " to "Thanks to you, more homes will run on clean energy!",
            "Bright Future! " to "Every quote you create brings us closer to a sustainable world!"
        )
        return quotes.random()
    }
}
