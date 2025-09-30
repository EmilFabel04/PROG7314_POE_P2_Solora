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
import dev.solora.quotes.FirebaseQuotesViewModel
import dev.solora.leads.LeadsViewModel
import dev.solora.data.FirebaseFirebaseQuote
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    
    private val quotesViewModel: FirebaseQuotesViewModel by viewModels()
    private val leadsViewModel: LeadsViewModel by viewModels()
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get references to UI elements
        val tvSavings = view.findViewById<TextView>(R.id.tv_savings)
        val tvFirebaseQuotes = view.findViewById<TextView>(R.id.tv_quotes)
        val tvLeads = view.findViewById<TextView>(R.id.tv_leads)
        val rvRecentFirebaseQuotes = view.findViewById<RecyclerView>(R.id.rv_recent_quotes)
        val cardEmptyFirebaseQuotes = view.findViewById<View>(R.id.card_empty_quotes)
        
        // Set up RecyclerView
        val quotesAdapter = RecentFirebaseQuotesAdapter { quote ->
            // Navigate to quote detail when clicked
            val bundle = Bundle().apply { putLong("id", quote.id) }
            findNavController().navigate(R.id.quoteDetailFragment, bundle)
        }
        
        rvRecentFirebaseQuotes.layoutManager = LinearLayoutManager(requireContext())
        rvRecentFirebaseQuotes.adapter = quotesAdapter
        
        // Observe quotes data
        viewLifecycleOwner.lifecycleScope.launch {
            quotesViewModel.quotes.collect { quotes ->
                // Update quote count
                tvFirebaseQuotes.text = "FirebaseQuotes ${quotes.size}"
                
                // Calculate total monthly savings
                val totalSavings = quotes.sumOf { it.savingsFirstYear }
                tvSavings.text = "R ${String.format("%.0f", totalSavings)}"
                
                // Update recent quotes (show max 3)
                val recentFirebaseQuotes = quotes.take(3)
                quotesAdapter.submitList(recentFirebaseQuotes)
                
                // Show/hide empty state
                if (quotes.isEmpty()) {
                    rvRecentFirebaseQuotes.visibility = View.GONE
                    cardEmptyFirebaseQuotes.visibility = View.VISIBLE
                } else {
                    rvRecentFirebaseQuotes.visibility = View.VISIBLE
                    cardEmptyFirebaseQuotes.visibility = View.GONE
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
        val calculateFirebaseQuoteCard = view.findViewById<View>(R.id.card_calculate_quote)
        calculateFirebaseQuoteCard?.setOnClickListener {
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
class RecentFirebaseQuotesAdapter(
    private val onFirebaseQuoteClick: (FirebaseQuote) -> Unit
) : RecyclerView.Adapter<RecentFirebaseQuotesAdapter.FirebaseQuoteViewHolder>() {
    
    private var quotes: List<FirebaseQuote> = emptyList()
    
    fun submitList(newFirebaseQuotes: List<FirebaseQuote>) {
        quotes = newFirebaseQuotes
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FirebaseQuoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_quote, parent, false)
        return FirebaseQuoteViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FirebaseQuoteViewHolder, position: Int) {
        holder.bind(quotes[position])
    }
    
    override fun getItemCount(): Int = quotes.size
    
    inner class FirebaseQuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvReference = itemView.findViewById<TextView>(R.id.tv_quote_reference)
        private val tvClient = itemView.findViewById<TextView>(R.id.tv_quote_client)
        private val tvSystem = itemView.findViewById<TextView>(R.id.tv_quote_system)
        private val tvSavings = itemView.findViewById<TextView>(R.id.tv_quote_savings)
        
        fun bind(quote: FirebaseQuote) {
            tvReference.text = quote.reference
            tvClient.text = quote.clientName
            tvSystem.text = "${String.format("%.1f", quote.systemKwp)} kW"
            tvSavings.text = "R${String.format("%.0f", quote.savingsFirstYear)}/month"
            
            itemView.setOnClickListener { onFirebaseQuoteClick(quote) }
        }
    }
}


