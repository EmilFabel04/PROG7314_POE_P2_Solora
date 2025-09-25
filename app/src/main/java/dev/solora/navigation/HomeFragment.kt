package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.solora.R
import dev.solora.quotes.QuotesViewModel
import dev.solora.leads.LeadsViewModel
import dev.solora.data.Quote
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    
    private val quotesViewModel: QuotesViewModel by viewModels()
    private val leadsViewModel: LeadsViewModel by viewModels()
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get references to UI elements
        val tvSavings = view.findViewById<TextView>(R.id.tv_savings)
        val tvQuotes = view.findViewById<TextView>(R.id.tv_quotes)
        val tvLeads = view.findViewById<TextView>(R.id.tv_leads)
        val rvRecentQuotes = view.findViewById<RecyclerView>(R.id.rv_recent_quotes)
        val cardEmptyQuotes = view.findViewById<View>(R.id.card_empty_quotes)
        
        // Set up RecyclerView
        val quotesAdapter = RecentQuotesAdapter { quote ->
            // Navigate to quote detail when clicked
            val bundle = Bundle().apply { putLong("id", quote.id) }
            findNavController().navigate(R.id.quoteDetailFragment, bundle)
        }
        
        rvRecentQuotes.layoutManager = LinearLayoutManager(requireContext())
        rvRecentQuotes.adapter = quotesAdapter
        
        // Observe quotes data
        viewLifecycleOwner.lifecycleScope.launch {
            quotesViewModel.quotes.collect { quotes ->
                // Update quote count
                tvQuotes.text = "Quotes ${quotes.size}"
                
                // Calculate total monthly savings
                val totalSavings = quotes.sumOf { it.savingsRands }
                tvSavings.text = "R ${String.format("%.0f", totalSavings)}"
                
                // Update recent quotes (show max 3)
                val recentQuotes = quotes.take(3)
                quotesAdapter.submitList(recentQuotes)
                
                // Show/hide empty state
                if (quotes.isEmpty()) {
                    rvRecentQuotes.visibility = View.GONE
                    cardEmptyQuotes.visibility = View.VISIBLE
                } else {
                    rvRecentQuotes.visibility = View.VISIBLE
                    cardEmptyQuotes.visibility = View.GONE
                }
            }
        }
        
        // Observe leads data
        viewLifecycleOwner.lifecycleScope.launch {
            leadsViewModel.leads.collect { leads ->
                tvLeads.text = "Leads ${leads.size}"
            }
        }
        
        // Set up navigation for action cards
        setupNavigation(view)
    }
    
    private fun setupNavigation(view: View) {
        // Hero section stats - clickable for navigation
        view.findViewById<View>(R.id.tv_quotes).setOnClickListener {
            findNavController().navigate(R.id.quotesFragment)
        }
        view.findViewById<View>(R.id.tv_leads).setOnClickListener {
            findNavController().navigate(R.id.leadsFragment)
        }
        
        // Quick action cards - make the entire cards clickable
        val calculateQuoteCard = view.findViewById<View>(R.id.card_calculate_quote)
        calculateQuoteCard?.setOnClickListener {
            findNavController().navigate(R.id.quotesFragment)
        }
        
        val viewLeadsCard = view.findViewById<View>(R.id.card_view_leads)
        viewLeadsCard?.setOnClickListener {
            findNavController().navigate(R.id.leadsFragment)
        }
        
        val notificationsCard = view.findViewById<View>(R.id.card_notifications)
        notificationsCard?.setOnClickListener {
            findNavController().navigate(R.id.action_to_notifications)
        }
        
        // Settings button
        view.findViewById<View>(R.id.btn_settings).setOnClickListener {
            findNavController().navigate(R.id.action_to_settings)
        }
        
        // Recent quotes section navigation
        view.findViewById<View>(R.id.tv_view_all_quotes).setOnClickListener {
            findNavController().navigate(R.id.quotesFragment)
        }
        
        view.findViewById<View>(R.id.btn_create_first_quote)?.setOnClickListener {
            findNavController().navigate(R.id.quotesFragment)
        }
        
        // Hidden buttons for backward compatibility
        view.findViewById<View>(R.id.btn_quotes).setOnClickListener { findNavController().navigate(R.id.quotesFragment) }
        view.findViewById<View>(R.id.btn_leads).setOnClickListener { findNavController().navigate(R.id.leadsFragment) }
        view.findViewById<View>(R.id.btn_notifications).setOnClickListener { findNavController().navigate(R.id.action_to_notifications) }
    }
}

// Simple adapter for recent quotes
class RecentQuotesAdapter(
    private val onQuoteClick: (Quote) -> Unit
) : RecyclerView.Adapter<RecentQuotesAdapter.QuoteViewHolder>() {
    
    private var quotes: List<Quote> = emptyList()
    
    fun submitList(newQuotes: List<Quote>) {
        quotes = newQuotes
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_quote, parent, false)
        return QuoteViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.bind(quotes[position])
    }
    
    override fun getItemCount(): Int = quotes.size
    
    inner class QuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvReference = itemView.findViewById<TextView>(R.id.tv_quote_reference)
        private val tvClient = itemView.findViewById<TextView>(R.id.tv_quote_client)
        private val tvSystem = itemView.findViewById<TextView>(R.id.tv_quote_system)
        private val tvSavings = itemView.findViewById<TextView>(R.id.tv_quote_savings)
        
        fun bind(quote: Quote) {
            tvReference.text = quote.reference
            tvClient.text = quote.clientName
            tvSystem.text = "${String.format("%.1f", quote.systemKw)} kW"
            tvSavings.text = "R${String.format("%.0f", quote.savingsRands)}/month"
            
            itemView.setOnClickListener { onQuoteClick(quote) }
        }
    }
}


