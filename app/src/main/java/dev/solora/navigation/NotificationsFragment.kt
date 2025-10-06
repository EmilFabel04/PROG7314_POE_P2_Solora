package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.solora.R

class NotificationsFragment : Fragment() {

    private lateinit var btnBackNotifications: ImageButton
    private lateinit var btnBackToHome: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        btnBackNotifications = view.findViewById(R.id.btn_back_notifications)
        btnBackToHome = view.findViewById(R.id.btn_back_to_home)
    }

    private fun setupClickListeners() {
        btnBackNotifications.setOnClickListener {
            findNavController().popBackStack()
        }

        btnBackToHome.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}