package dev.solora.navigation

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import dev.solora.R

class NotificationsFragment : Fragment() {
    
    private lateinit var btnClearAll: Button
    private lateinit var notificationQuote: MaterialCardView
    private lateinit var notificationLead: MaterialCardView
    private lateinit var notificationSystem: MaterialCardView
    private lateinit var layoutEmptyNotifications: View
    private lateinit var btnDismissQuote: Button
    private lateinit var btnDismissLead: Button
    private lateinit var btnDismissSystem: Button
    
    private var notificationCount = 3 // Track visible notifications
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
        updateEmptyState()
    }
    
    private fun initializeViews(view: View) {
        btnClearAll = view.findViewById(R.id.btn_clear_all)
        notificationQuote = view.findViewById(R.id.notification_quote)
        notificationLead = view.findViewById(R.id.notification_lead)
        notificationSystem = view.findViewById(R.id.notification_system)
        layoutEmptyNotifications = view.findViewById(R.id.layout_empty_notifications)
        btnDismissQuote = view.findViewById(R.id.btn_dismiss_quote)
        btnDismissLead = view.findViewById(R.id.btn_dismiss_lead)
        btnDismissSystem = view.findViewById(R.id.btn_dismiss_system)
    }
    
    private fun setupClickListeners() {
        // Clear all notifications
        btnClearAll.setOnClickListener {
            showClearAllDialog()
        }
        
        // Individual dismiss buttons
        btnDismissQuote.setOnClickListener {
            dismissNotification(notificationQuote, "Quote notification dismissed")
        }
        
        btnDismissLead.setOnClickListener {
            dismissNotification(notificationLead, "Lead notification dismissed")
        }
        
        btnDismissSystem.setOnClickListener {
            dismissNotification(notificationSystem, "System notification dismissed")
        }
        
        // Make notification cards clickable for actions
        notificationQuote.setOnClickListener {
            Toast.makeText(requireContext(), "Opening quote details...", Toast.LENGTH_SHORT).show()
            // In a real app, you'd navigate to the specific quote
            // findNavController().navigate(R.id.action_to_quote_detail, bundle)
        }
        
        notificationLead.setOnClickListener {
            Toast.makeText(requireContext(), "Opening lead details...", Toast.LENGTH_SHORT).show()
            // In a real app, you'd navigate to the specific lead
            // findNavController().navigate(R.id.leadsFragment)
        }
        
        notificationSystem.setOnClickListener {
            Toast.makeText(requireContext(), "System update details", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showClearAllDialog() {
        if (notificationCount == 0) {
            Toast.makeText(requireContext(), "No notifications to clear", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Clear All Notifications")
            .setMessage("Are you sure you want to clear all notifications? This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                clearAllNotifications()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun clearAllNotifications() {
        // Hide all notification cards
        notificationQuote.visibility = View.GONE
        notificationLead.visibility = View.GONE
        notificationSystem.visibility = View.GONE
        
        notificationCount = 0
        updateEmptyState()
        
        Toast.makeText(requireContext(), "All notifications cleared", Toast.LENGTH_SHORT).show()
    }
    
    private fun dismissNotification(notificationCard: MaterialCardView, message: String) {
        // Animate the dismissal
        notificationCard.animate()
            .alpha(0f)
            .translationX(notificationCard.width.toFloat())
            .setDuration(300)
            .withEndAction {
                notificationCard.visibility = View.GONE
                notificationCount--
                updateEmptyState()
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
            .start()
    }
    
    private fun updateEmptyState() {
        if (notificationCount == 0) {
            layoutEmptyNotifications.visibility = View.VISIBLE
            btnClearAll.isEnabled = false
            btnClearAll.alpha = 0.5f
        } else {
            layoutEmptyNotifications.visibility = View.GONE
            btnClearAll.isEnabled = true
            btnClearAll.alpha = 1.0f
        }
    }
}


