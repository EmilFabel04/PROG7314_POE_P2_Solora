package dev.solora.navigation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dev.solora.R

// QuotesListAdapter for RecyclerView
class QuotesListAdapter(
    private val onQuoteClick: (dev.solora.data.FirebaseQuote) -> Unit
) : androidx.recyclerview.widget.ListAdapter<dev.solora.data.FirebaseQuote, QuotesListAdapter.QuoteViewHolder>(QuoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quote, parent, false)
        return QuoteViewHolder(view, onQuoteClick)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class QuoteViewHolder(
        itemView: View,
        private val onQuoteClick: (dev.solora.data.FirebaseQuote) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

        private val tvReference: TextView = itemView.findViewById(R.id.tv_quote_reference)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_quote_date)
        private val tvClientName: TextView = itemView.findViewById(R.id.tv_client_name)
        private val tvClientAddress: TextView = itemView.findViewById(R.id.tv_client_address)
        private val tvSystemSize: TextView = itemView.findViewById(R.id.tv_system_size)
        private val tvSavings: TextView = itemView.findViewById(R.id.tv_savings)

        fun bind(quote: dev.solora.data.FirebaseQuote) {
            tvReference.text = quote.reference
            tvClientName.text = quote.clientName
            tvClientAddress.text = quote.address
            tvSystemSize.text = "${String.format("%.1f", quote.systemKwp)} kW"
            tvSavings.text = "R ${String.format("%.0f", quote.monthlySavings)}"

            // Format date
            val dateText = quote.createdAt?.toDate()?.let {
                java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(it)
            } ?: "Unknown"
            tvDate.text = dateText

            itemView.setOnClickListener {
                onQuoteClick(quote)
            }
        }
    }

    class QuoteDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<dev.solora.data.FirebaseQuote>() {
        override fun areItemsTheSame(oldItem: dev.solora.data.FirebaseQuote, newItem: dev.solora.data.FirebaseQuote): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: dev.solora.data.FirebaseQuote, newItem: dev.solora.data.FirebaseQuote): Boolean {
            return oldItem == newItem
        }
    }
}