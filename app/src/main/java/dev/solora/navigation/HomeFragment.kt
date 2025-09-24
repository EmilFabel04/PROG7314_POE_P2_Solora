package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.solora.R

class HomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.btn_quotes).setOnClickListener { findNavController().navigate(R.id.quotesFragment) }
        view.findViewById<View>(R.id.btn_leads).setOnClickListener { findNavController().navigate(R.id.leadsFragment) }
        view.findViewById<View>(R.id.btn_notifications).setOnClickListener { findNavController().navigate(R.id.action_to_notifications) }
        view.findViewById<View>(R.id.btn_settings).setOnClickListener { findNavController().navigate(R.id.action_to_settings) }
    }
}


