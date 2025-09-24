package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dev.solora.R
import dev.solora.quotes.QuotesViewModel

class QuoteDetailFragment : Fragment() {
    private val quotesViewModel: QuotesViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_quote_detail, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val id = requireArguments().getLong("id", 0L)
        view.findViewById<TextView>(R.id.tv_reference).text = "Quote #$id"
        view.findViewById<TextView>(R.id.tv_summary).text = "Summary will appear here."
        view.findViewById<Button>(R.id.btn_export_pdf).setOnClickListener { findNavController().popBackStack() }
    }
}


