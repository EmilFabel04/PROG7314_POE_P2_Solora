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
import dev.solora.data.FirebaseQuote
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    
    private val quotesViewModel: QuotesViewModel by viewModels()
    private val leadsViewModel: LeadsViewModel by viewModels()
    
    // UI Elements
    private lateinit var tvCompanyName: TextView
    private lateinit var tvConsultantName: TextView
    private lateinit var tvQuotesCount: TextView
    private lateinit var tvLeadsCount: TextView
    private lateinit var rvRecentQuotes: RecyclerView
    private lateinit var cardEmptyQuotes: View
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners(view)
        setupRecyclerView()
        observeData()
    }
    
    private fun initializeViews(view: View) {
        tvCompanyName = view.findViewById(R.id.tv_company_name)
        tvConsultantName = view.findViewById(R.id.tv_consultant_name)
        tvQuotesCount = view.findViewById(R.id.tv_quotes_count)
        tvLeadsCount = view.findViewById(R.id.tv_leads_count)
        rvRecentQuotes = view.findViewById(R.id.rv_recent_quotes)
        cardEmptyQuotes = view.findViewById(R.id.card_empty_quotes)
    }
    
    private fun setupClickListeners(view: View) {
        // Header buttons
        view.findViewById<View>(R.id.btn_notifications)?.setOnClickListener {
            findNavController().navigate(R.id.action_to_notifications)
        }
        
        view.findViewById<View>(R.id.btn_settings)?.setOnClickListener {
            findNavController().navigate(R.id.action_to_settings)
        }
        
        // Action cards
        view.findViewById<View>(R.id.card_add_leads)?.setOnClickListener {
            findNavController().navigate(R.id.leadsFragment)
        }
        
        view.findViewById<View>(R.id.card_calculate_quote)?.setOnClickListener {
            findNavController().navigate(R.id.quotesFragment)
        }
        
        // Recent quotes section
        view.findViewById<View>(R.id.tv_view_all_quotes)?.setOnClickListener {
            findNavController().navigate(R.id.quotesFragment)
        }
        
        view.findViewById<View>(R.id.btn_create_first_quote)?.setOnClickListener {
            findNavController().navigate(R.id.quotesFragment)
        }
    }
    
    private fun setupRecyclerView() {
        val quotesAdapter = RecentQuotesAdapter { quote ->
            // Navigate to quote detail when clicked
            val bundle = Bundle().apply { putString("id", quote.id) }
            findNavController().navigate(R.id.quoteDetailFragment, bundle)
        }
        
        rvRecentQuotes.layoutManager = LinearLayoutManager(requireContext())
        rvRecentQuotes.adapter = quotesAdapter
    }
    
    private fun observeData() {
        // Observe quotes data
        viewLifecycleOwner.lifecycleScope.launch {
            quotesViewModel.quotes.collect { quotes ->
                tvQuotesCount.text = quotes.size.toString()
                
                // Show recent quotes (limit to 5)
                val recentQuotes = quotes.take(5)
                (rvRecentQuotes.adapter as? RecentQuotesAdapter)?.submitList(recentQuotes)
                
                // Show/hide empty state
                if (recentQuotes.isEmpty()) {
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
                tvLeadsCount.text = leads.size.toString()
            }
        }
    }
}

// Simple adapter for recent quotes
class RecentQuotesAdapter(
    private val onQuoteClick: (FirebaseQuote) -> Unit
) : RecyclerView.Adapter<RecentQuotesAdapter.QuoteViewHolder>() {
    
    private var quotes: List<FirebaseQuote> = emptyList()
    
    fun submitList(newQuotes: List<FirebaseQuote>) {
        quotes = newQuotes
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_quote, parent, false)
        return QuoteViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.bind(quotes[position], onQuoteClick)
    }
    
    override fun getItemCount(): Int = quotes.size
    
    class QuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAddress: TextView = itemView.findViewById(R.id.tv_address)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        
        fun bind(quote: FirebaseQuote, onQuoteClick: (FirebaseQuote) -> Unit) {
            tvAddress.text = quote.address.ifEmpty { "Unknown Address" }
            tvAmount.text = "R${quote.monthlySavings.toInt()}"
            
            itemView.setOnClickListener {
                onQuoteClick(quote)
            }
        }
    }
}