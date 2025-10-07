package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dev.solora.R
import dev.solora.quote.QuoteOutputs
import dev.solora.quotes.QuotesViewModel

class QuoteResultsFragment : Fragment() {
    
    // ViewModels
    private val quotesViewModel: QuotesViewModel by viewModels()
    
    // UI Components
    private lateinit var btnBack: ImageButton
    private lateinit var tvNumberOfPanels: TextView
    private lateinit var tvTotalSystemSize: TextView
    private lateinit var tvRecommendedInverter: TextView
    private lateinit var tvEstimatedSavings: TextView
    private lateinit var btnSaveQuote: Button
    
    private var calculationOutputs: QuoteOutputs? = null
    private var calculatedAddress: String = ""
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_quote_results, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
        setupBackPressedCallback()
        
        // Get data from arguments
        arguments?.let { args ->
            calculationOutputs = args.getSerializable("calculation_outputs") as? QuoteOutputs
            calculatedAddress = args.getString("calculated_address") ?: ""
        }
        
        displayResults()
    }
    
    private fun initializeViews(view: View) {
        btnBack = view.findViewById(R.id.btn_back_results)
        tvNumberOfPanels = view.findViewById(R.id.tv_number_of_panels)
        tvTotalSystemSize = view.findViewById(R.id.tv_total_system_size)
        tvRecommendedInverter = view.findViewById(R.id.tv_recommended_inverter)
        tvEstimatedSavings = view.findViewById(R.id.tv_estimated_savings)
        btnSaveQuote = view.findViewById(R.id.btn_save_quote)
        
    }
    
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            cancelQuoteAndGoBack()
        }
        
        btnSaveQuote.setOnClickListener {
            navigateToClientDetails()
        }
    }
    
    private fun setupBackPressedCallback() {
        // Handle system back button press
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                cancelQuoteAndGoBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }
    
    private fun cancelQuoteAndGoBack() {
        
        // Clear the calculated quote when going back (cancelling the quote)
        quotesViewModel.clearLastQuote()
        
        try {
            // Navigate directly to quotes fragment
            findNavController().navigate(R.id.action_quote_results_to_quotes)
        } catch (e: Exception) {
        }
    }
    
    private fun displayResults() {
        calculationOutputs?.let { outputs ->
            tvNumberOfPanels.text = "${outputs.panels}"
            tvTotalSystemSize.text = "${String.format("%.1f", outputs.systemKw)} kW"
            tvRecommendedInverter.text = "${String.format("%.1f", outputs.systemKw * 0.8)} kW"
            tvEstimatedSavings.text = "R ${String.format("%.2f", outputs.monthlySavingsRands)}"
        }
    }
    
    private fun navigateToClientDetails() {
        val bundle = Bundle().apply {
            putSerializable("calculation_outputs", calculationOutputs)
            putString("calculated_address", calculatedAddress)
        }
        findNavController().navigate(R.id.clientDetailsFragment, bundle)
    }
}
