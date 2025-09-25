package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dev.solora.R
import dev.solora.quotes.QuotesViewModel
import dev.solora.quotes.CalculationState
import kotlinx.coroutines.launch

class QuotesFragment : Fragment() {
    private val quotesViewModel: QuotesViewModel by viewModels()
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_quotes, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get UI references
        val etRef = view.findViewById<EditText>(R.id.et_ref)
        val etClient = view.findViewById<EditText>(R.id.et_client)
        val etAddress = view.findViewById<EditText>(R.id.et_address)
        val etUsage = view.findViewById<EditText>(R.id.et_usage)
        val etBill = view.findViewById<EditText>(R.id.et_bill)
        val etTariff = view.findViewById<EditText>(R.id.et_tariff)
        val etPanel = view.findViewById<EditText>(R.id.et_panel)
        val btnCalculate = view.findViewById<Button>(R.id.btn_calculate)
        val btnOpenDetail = view.findViewById<Button>(R.id.btn_open_detail)
        val btnTestFirebase = view.findViewById<Button>(R.id.btn_test_firebase)
        val btnSyncFirebase = view.findViewById<Button>(R.id.btn_sync_firebase)
        val tvLatest = view.findViewById<TextView>(R.id.tv_latest)
        
        // Observe calculation state
        viewLifecycleOwner.lifecycleScope.launch {
            quotesViewModel.calculationState.collect { state ->
                when (state) {
                    is CalculationState.Idle -> {
                        btnCalculate.isEnabled = true
                        btnCalculate.text = "Calculate & Save"
                    }
                    is CalculationState.Loading -> {
                        btnCalculate.isEnabled = false
                        btnCalculate.text = "Calculating..."
                    }
                    is CalculationState.Success -> {
                        btnCalculate.isEnabled = true
                        btnCalculate.text = "Calculate & Save"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        
                        // Navigate to quote detail after successful calculation
                        quotesViewModel.lastQuote.value?.let { quote ->
                            val bundle = Bundle().apply { putLong("id", quote.id) }
                            findNavController().navigate(R.id.quoteDetailFragment, bundle)
                        }
                    }
                    is CalculationState.Error -> {
                        btnCalculate.isEnabled = true
                        btnCalculate.text = "Calculate & Save"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        // Observe last quote for display
        viewLifecycleOwner.lifecycleScope.launch {
            quotesViewModel.lastQuote.collect { quote ->
                if (quote != null) {
                    tvLatest.text = "Reference: ${quote.reference}\n" +
                            "Client: ${quote.clientName}\n" +
                            "System: ${quote.systemKw} kW (${quote.panels} panels)\n" +
                            "Estimated savings: R${String.format("%.2f", quote.savingsRands)}/month"
                } else {
                    tvLatest.text = "No calculations yet"
                }
            }
        }
        
        // Handle calculate button
        btnCalculate.setOnClickListener {
            val reference = etRef.text?.toString()?.trim() ?: ""
            val clientName = etClient.text?.toString()?.trim() ?: ""
            val address = etAddress.text?.toString()?.trim() ?: ""
            val usageText = etUsage.text?.toString()?.trim() ?: ""
            val billText = etBill.text?.toString()?.trim() ?: ""
            val tariffText = etTariff.text?.toString()?.trim() ?: "2.50"
            val panelText = etPanel.text?.toString()?.trim() ?: "420"
            
            // Validation
            if (reference.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a quote reference", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (clientName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter client name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (address.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (usageText.isEmpty() && billText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter either monthly usage or average bill", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            try {
                val usage = if (usageText.isNotEmpty()) usageText.toDouble() else null
                val bill = if (billText.isNotEmpty()) billText.toDouble() else null
                val tariff = tariffText.toDouble()
                val panelWatt = panelText.toInt()
                
                // Perform calculation using address for better accuracy
                quotesViewModel.calculateAndSaveUsingAddress(
                    reference = reference,
                    clientName = clientName,
                    address = address,
                    usageKwh = usage,
                    billRands = bill,
                    tariff = tariff,
                    panelWatt = panelWatt
                )
                
            } catch (e: NumberFormatException) {
                Toast.makeText(requireContext(), "Please check your numeric inputs", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Handle demo detail button
        btnOpenDetail.setOnClickListener {
            quotesViewModel.lastQuote.value?.let { quote ->
                val bundle = Bundle().apply { putLong("id", quote.id) }
                findNavController().navigate(R.id.quoteDetailFragment, bundle)
            } ?: run {
                Toast.makeText(requireContext(), "No quote to display. Calculate one first!", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Handle Firebase test button
        btnTestFirebase.setOnClickListener {
            quotesViewModel.testFirebaseConnection()
        }
        
        // Handle Firebase sync button
        btnSyncFirebase.setOnClickListener {
            quotesViewModel.syncToFirebase()
        }
    }
}


