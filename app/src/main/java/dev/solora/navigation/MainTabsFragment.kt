package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.solora.R

class MainTabsFragment : Fragment() {
    private lateinit var childNavController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main_tabs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navHost = childFragmentManager.findFragmentById(R.id.tabs_nav_host) as NavHostFragment
        childNavController = navHost.navController
        
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(childNavController)
        
        // Apply orange theming to bottom navigation
        bottomNav.itemIconTintList = null // Disable default tinting
        bottomNav.itemTextColor = ContextCompat.getColorStateList(requireContext(), R.drawable.bottom_nav_text_color)
    }
}


