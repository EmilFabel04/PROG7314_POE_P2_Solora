package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import dev.solora.R

class NotificationsFragment : Fragment() {
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up click listener for the notification card
        setupClickListeners(view)
        
        // Show toast message when notifications fragment is loaded
        showComingSoonToast()
    }
    
    private fun setupClickListeners(view: View) {
        try {
            // Find the notification card and set up click listener
            val notificationCard = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.notification_card)
            notificationCard?.setOnClickListener {
                showComingSoonToast()
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationsFragment", "Error setting up click listeners: ${e.message}", e)
        }
    }
    
    private fun showComingSoonToast() {
        try {
            context?.let { ctx ->
                Toast.makeText(
                    ctx, 
                    "Push notifications coming soon!", 
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationsFragment", "Error showing toast: ${e.message}", e)
        }
    }
}


