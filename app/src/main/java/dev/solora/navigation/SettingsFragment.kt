package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import dev.solora.R
import dev.solora.settings.SettingsViewModel
import dev.solora.settings.CalculationSettings
import dev.solora.settings.CompanySettings
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    
    private val settingsViewModel: SettingsViewModel by viewModels()
    
    // Tab elements
    private lateinit var tabCalculation: TextView
    private lateinit var tabCompany: TextView
    private lateinit var contentCalculation: View
    private lateinit var contentCompany: View
    
    // Calculation form elements
    private lateinit var etDefaultTariff: TextInputEditText
    private lateinit var etDefaultPanelWatt: TextInputEditText
    private lateinit var etDefaultSunHours: TextInputEditText
    private lateinit var etPanelCostPerWatt: TextInputEditText
    private lateinit var etInverterCostPerWatt: TextInputEditText
    private lateinit var etInstallationCostPerKw: TextInputEditText
    private lateinit var etPanelEfficiency: TextInputEditText
    private lateinit var etPerformanceRatio: TextInputEditText
    private lateinit var etInverterSizingRatio: TextInputEditText
    private lateinit var etSystemLifetime: TextInputEditText
    
    // Company form elements
    private lateinit var etCompanyName: TextInputEditText
    private lateinit var etCompanyAddress: TextInputEditText
    private lateinit var etCompanyPhone: TextInputEditText
    private lateinit var etCompanyEmail: TextInputEditText
    private lateinit var etCompanyWebsite: TextInputEditText
    private lateinit var etConsultantName: TextInputEditText
    private lateinit var etConsultantPhone: TextInputEditText
    private lateinit var etConsultantEmail: TextInputEditText
    private lateinit var etConsultantLicense: TextInputEditText
    
    // Action buttons
    private lateinit var btnSaveSettings: Button
    private lateinit var btnResetSettings: Button
    
    private var currentTab = 0 // 0 = Calculation, 1 = Company
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            android.util.Log.d("SettingsFragment", "Starting SettingsFragment initialization")
            initializeViews(view)
            android.util.Log.d("SettingsFragment", "Views initialized successfully")
            setupTabs()
            android.util.Log.d("SettingsFragment", "Tabs setup successfully")
            setupButtons()
            android.util.Log.d("SettingsFragment", "Buttons setup successfully")
            observeSettings()
            android.util.Log.d("SettingsFragment", "Settings observer setup successfully")
            
            android.util.Log.d("SettingsFragment", "Settings fragment initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "Error initializing SettingsFragment", e)
            Toast.makeText(requireContext(), "Error initializing settings: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun initializeViews(view: View) {
        // Tab elements
        tabCalculation = view.findViewById(R.id.tab_calculation)
        tabCompany = view.findViewById(R.id.tab_company)
        contentCalculation = view.findViewById(R.id.content_calculation)
        contentCompany = view.findViewById(R.id.content_company)
        
        // Calculation form elements
        etDefaultTariff = view.findViewById(R.id.et_default_tariff)
        etDefaultPanelWatt = view.findViewById(R.id.et_default_panel_watt)
        etDefaultSunHours = view.findViewById(R.id.et_default_sun_hours)
        etPanelCostPerWatt = view.findViewById(R.id.et_panel_cost_per_watt)
        etInverterCostPerWatt = view.findViewById(R.id.et_inverter_cost_per_watt)
        etInstallationCostPerKw = view.findViewById(R.id.et_installation_cost_per_kw)
        etPanelEfficiency = view.findViewById(R.id.et_panel_efficiency)
        etPerformanceRatio = view.findViewById(R.id.et_performance_ratio)
        etInverterSizingRatio = view.findViewById(R.id.et_inverter_sizing_ratio)
        etSystemLifetime = view.findViewById(R.id.et_system_lifetime)
        
        // Company form elements
        etCompanyName = view.findViewById(R.id.et_company_name)
        etCompanyAddress = view.findViewById(R.id.et_company_address)
        etCompanyPhone = view.findViewById(R.id.et_company_phone)
        etCompanyEmail = view.findViewById(R.id.et_company_email)
        etCompanyWebsite = view.findViewById(R.id.et_company_website)
        etConsultantName = view.findViewById(R.id.et_consultant_name)
        etConsultantPhone = view.findViewById(R.id.et_consultant_phone)
        etConsultantEmail = view.findViewById(R.id.et_consultant_email)
        etConsultantLicense = view.findViewById(R.id.et_consultant_license)
        
        // Action buttons
        btnSaveSettings = view.findViewById(R.id.btn_save_settings)
        btnResetSettings = view.findViewById(R.id.btn_reset_settings)
    }
    
    private fun setupTabs() {
        tabCalculation.setOnClickListener {
            switchToTab(0)
        }
        
        tabCompany.setOnClickListener {
            switchToTab(1)
        }
        
        updateTabAppearance()
    }
    
    private fun switchToTab(tab: Int) {
        currentTab = tab
        
        when (tab) {
            0 -> {
                contentCalculation.visibility = View.VISIBLE
                contentCompany.visibility = View.GONE
            }
            1 -> {
                contentCalculation.visibility = View.GONE
                contentCompany.visibility = View.VISIBLE
            }
        }
        
        updateTabAppearance()
    }
    
    private fun updateTabAppearance() {
        when (currentTab) {
            0 -> {
                tabCalculation.setBackgroundResource(R.drawable.tab_selected)
                tabCalculation.alpha = 1.0f
                tabCompany.setBackgroundResource(R.drawable.tab_unselected)
                tabCompany.alpha = 0.7f
            }
            1 -> {
                tabCompany.setBackgroundResource(R.drawable.tab_selected)
                tabCompany.alpha = 1.0f
                tabCalculation.setBackgroundResource(R.drawable.tab_unselected)
                tabCalculation.alpha = 0.7f
            }
        }
    }
    
    private fun setupButtons() {
        btnSaveSettings.setOnClickListener {
            saveSettings()
        }
        
        btnResetSettings.setOnClickListener {
            resetToDefaults()
        }
    }
    
    private fun observeSettings() {
        try {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    android.util.Log.d("SettingsFragment", "Starting to observe settings")
                    settingsViewModel.settings.collect { settings ->
                        android.util.Log.d("SettingsFragment", "Settings received: $settings")
                        populateForms(settings)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SettingsFragment", "Error observing settings", e)
                    Toast.makeText(requireContext(), "Error loading settings: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "Error setting up settings observer", e)
            Toast.makeText(requireContext(), "Error setting up settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun populateForms(settings: dev.solora.settings.AppSettings) {
        try {
            android.util.Log.d("SettingsFragment", "Populating forms with settings")
            
            // Calculation settings
            etDefaultTariff.setText(settings.calculationSettings.defaultTariff.toString())
            etDefaultPanelWatt.setText(settings.calculationSettings.defaultPanelWatt.toString())
            etDefaultSunHours.setText(settings.calculationSettings.defaultSunHours.toString())
            etPanelCostPerWatt.setText(settings.calculationSettings.panelCostPerWatt.toString())
            etInverterCostPerWatt.setText(settings.calculationSettings.inverterCostPerWatt.toString())
            etInstallationCostPerKw.setText(settings.calculationSettings.installationCostPerKw.toString())
            etPanelEfficiency.setText((settings.calculationSettings.panelEfficiency * 100).toString())
            etPerformanceRatio.setText((settings.calculationSettings.performanceRatio * 100).toString())
            etInverterSizingRatio.setText((settings.calculationSettings.inverterSizingRatio * 100).toString())
            etSystemLifetime.setText(settings.calculationSettings.systemLifetime.toString())
            
            // Company settings
            etCompanyName.setText(settings.companySettings.companyName)
            etCompanyAddress.setText(settings.companySettings.companyAddress)
            etCompanyPhone.setText(settings.companySettings.companyPhone)
            etCompanyEmail.setText(settings.companySettings.companyEmail)
            etCompanyWebsite.setText(settings.companySettings.companyWebsite)
            etConsultantName.setText(settings.companySettings.consultantName)
            etConsultantPhone.setText(settings.companySettings.consultantPhone)
            etConsultantEmail.setText(settings.companySettings.consultantEmail)
            etConsultantLicense.setText(settings.companySettings.consultantLicense)
            
            android.util.Log.d("SettingsFragment", "Forms populated successfully")
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "Error populating forms", e)
            Toast.makeText(requireContext(), "Error loading form data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveSettings() {
        try {
            // Get calculation settings
            val calculationSettings = CalculationSettings(
                defaultTariff = etDefaultTariff.text.toString().toDoubleOrNull() ?: 2.50,
                defaultPanelWatt = etDefaultPanelWatt.text.toString().toIntOrNull() ?: 420,
                defaultSunHours = etDefaultSunHours.text.toString().toDoubleOrNull() ?: 5.0,
                panelCostPerWatt = etPanelCostPerWatt.text.toString().toDoubleOrNull() ?: 15.0,
                inverterCostPerWatt = etInverterCostPerWatt.text.toString().toDoubleOrNull() ?: 12.0,
                installationCostPerKw = etInstallationCostPerKw.text.toString().toDoubleOrNull() ?: 15000.0,
                panelEfficiency = (etPanelEfficiency.text.toString().toDoubleOrNull() ?: 20.0) / 100.0,
                performanceRatio = (etPerformanceRatio.text.toString().toDoubleOrNull() ?: 80.0) / 100.0,
                inverterSizingRatio = (etInverterSizingRatio.text.toString().toDoubleOrNull() ?: 80.0) / 100.0,
                systemLifetime = etSystemLifetime.text.toString().toIntOrNull() ?: 25,
                panelDegradationRate = 0.005, // Keep default
                co2PerKwh = 0.5 // Keep default
            )
            
            // Get company settings
            val companySettings = CompanySettings(
                companyName = etCompanyName.text.toString().trim(),
                companyAddress = etCompanyAddress.text.toString().trim(),
                companyPhone = etCompanyPhone.text.toString().trim(),
                companyEmail = etCompanyEmail.text.toString().trim(),
                companyWebsite = etCompanyWebsite.text.toString().trim(),
                consultantName = etConsultantName.text.toString().trim(),
                consultantPhone = etConsultantPhone.text.toString().trim(),
                consultantEmail = etConsultantEmail.text.toString().trim(),
                consultantLicense = etConsultantLicense.text.toString().trim(),
                companyLogo = "", // Keep empty for now
                quoteFooter = "", // Keep empty for now
                termsAndConditions = "" // Keep empty for now
            )
            
            // Save settings
            settingsViewModel.updateCalculationSettings(calculationSettings)
            settingsViewModel.updateCompanySettings(companySettings)
            
            Toast.makeText(requireContext(), "Settings saved successfully!", Toast.LENGTH_SHORT).show()
            android.util.Log.d("SettingsFragment", "Settings saved successfully")
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error saving settings: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("SettingsFragment", "Error saving settings", e)
        }
    }
    
    private fun resetToDefaults() {
        settingsViewModel.resetToDefaults()
        Toast.makeText(requireContext(), "Settings reset to defaults", Toast.LENGTH_SHORT).show()
        android.util.Log.d("SettingsFragment", "Settings reset to defaults")
    }
}