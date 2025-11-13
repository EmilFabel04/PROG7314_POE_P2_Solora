package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dev.solora.R
import dev.solora.notifications.MotivationalNotificationManager
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private lateinit var btnBackNotifications: ImageButton
    private lateinit var btnBackToHome: Button
    private lateinit var switchNotifications: Switch
    private lateinit var tvNotificationStatus: TextView
    private lateinit var btnTestNotification: Button
    
    private lateinit var notificationManager: MotivationalNotificationManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        notificationManager = MotivationalNotificationManager(requireContext())
        
        initializeViews(view)
        setupClickListeners()
        loadNotificationSettings()
    }

    private fun initializeViews(view: View) {
        btnBackNotifications = view.findViewById(R.id.btn_back_notifications)
        btnBackToHome = view.findViewById(R.id.btn_back_to_home)
        switchNotifications = view.findViewById(R.id.switch_notifications) ?: return
        tvNotificationStatus = view.findViewById(R.id.tv_notification_status) ?: return
        btnTestNotification = view.findViewById(R.id.btn_test_notification) ?: return
    }

    private fun setupClickListeners() {
        btnBackNotifications.setOnClickListener {
            findNavController().popBackStack()
        }

        btnBackToHome.setOnClickListener {
            findNavController().popBackStack()
        }
        
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            handleNotificationToggle(isChecked)
        }
        
        btnTestNotification.setOnClickListener {
            sendTestNotification()
        }
    }
    
    private fun loadNotificationSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            val isEnabled = notificationManager.isNotificationsEnabled()
            switchNotifications.isChecked = isEnabled
            updateNotificationStatus(isEnabled)
        }
    }
    
    private fun handleNotificationToggle(isEnabled: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            notificationManager.enableMotivationalNotifications(isEnabled)
            updateNotificationStatus(isEnabled)
            
            val message = if (isEnabled) {
                "Motivational notifications enabled!"
            } else {
                "Motivational notifications disabled"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateNotificationStatus(isEnabled: Boolean) {
        tvNotificationStatus.text = if (isEnabled) {
            "Notifications are enabled - you'll receive motivational messages!"
        } else {
            "Notifications are disabled"
        }
    }
    
    private fun sendTestNotification() {
        viewLifecycleOwner.lifecycleScope.launch {
            notificationManager.checkAndSendMotivationalMessage()
            Toast.makeText(requireContext(), "Test notification sent!", Toast.LENGTH_SHORT).show()
        }
    }
}