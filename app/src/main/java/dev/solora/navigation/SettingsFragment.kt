package dev.solora.navigation

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.solora.R

class SettingsFragment : Fragment() {
    
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var swPush: Switch
    private lateinit var swSummary: Switch
    private lateinit var tvLanguage: TextView
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize SharedPreferences
        sharedPrefs = requireContext().getSharedPreferences("solora_settings", Context.MODE_PRIVATE)
        
        initializeViews(view)
        loadSettings()
        setupClickListeners()
    }
    
    private fun initializeViews(view: View) {
        swPush = view.findViewById(R.id.sw_push)
        swSummary = view.findViewById(R.id.sw_summary)
        tvLanguage = view.findViewById(R.id.tv_language)
    }
    
    private fun loadSettings() {
        // Load notification settings
        swPush.isChecked = sharedPrefs.getBoolean("push_notifications", true)
        swSummary.isChecked = sharedPrefs.getBoolean("summary_emails", false)
        
        // Load language setting
        val currentLanguage = sharedPrefs.getString("app_language", "English") ?: "English"
        tvLanguage.text = currentLanguage
    }
    
    private fun setupClickListeners() {
        // Push notifications toggle
        swPush.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("push_notifications", isChecked).apply()
            val message = if (isChecked) "Push notifications enabled" else "Push notifications disabled"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        
        // Summary emails toggle
        swSummary.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("summary_emails", isChecked).apply()
            val message = if (isChecked) "Summary emails enabled" else "Summary emails disabled"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        
        // Language selection
        tvLanguage.setOnClickListener {
            showLanguageDialog()
        }
        
        // Make language card clickable
        view?.findViewById<View>(R.id.tv_language)?.parent?.let { parent ->
            if (parent is View) {
                parent.setOnClickListener {
                    showLanguageDialog()
                }
            }
        }
    }
    
    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Afrikaans", "isiXhosa")
        val currentLanguage = sharedPrefs.getString("app_language", "English") ?: "English"
        val currentIndex = languages.indexOf(currentLanguage)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Language")
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                val selectedLanguage = languages[which]
                
                // Save language preference
                sharedPrefs.edit().putString("app_language", selectedLanguage).apply()
                
                // Update UI
                tvLanguage.text = selectedLanguage
                
                // Show confirmation
                Toast.makeText(requireContext(), "Language changed to $selectedLanguage", Toast.LENGTH_SHORT).show()
                
                dialog.dismiss()
                
                // Note: In a real app, you'd restart the activity or apply locale changes here
                // For now, we'll just show a message
                if (selectedLanguage != "English") {
                    Toast.makeText(requireContext(), "Language will be applied on next app restart", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}


