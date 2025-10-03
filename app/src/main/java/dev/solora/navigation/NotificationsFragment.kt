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
        android.util.Log.d("NotificationsFragment", "onCreateView called")
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("NotificationsFragment", "onViewCreated called")
        
        // Simple click listener setup
        try {
            val notificationCard = view.findViewById<View>(R.id.notification_card)
            notificationCard?.setOnClickListener {
                android.util.Log.d("NotificationsFragment", "Notification card clicked")
                showToast()
            }
            android.util.Log.d("NotificationsFragment", "Click listener set up successfully")
        } catch (e: Exception) {
            android.util.Log.e("NotificationsFragment", "Error setting up click listener: ${e.message}", e)
        }
    }
    
    private fun showToast() {
        try {
            if (isAdded && context != null) {
                Toast.makeText(
                    requireContext(), 
                    "Push notifications coming soon!", 
                    Toast.LENGTH_LONG
                ).show()
                android.util.Log.d("NotificationsFragment", "Toast shown successfully")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationsFragment", "Error showing toast: ${e.message}", e)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        android.util.Log.d("NotificationsFragment", "onDestroyView called")
    }
}