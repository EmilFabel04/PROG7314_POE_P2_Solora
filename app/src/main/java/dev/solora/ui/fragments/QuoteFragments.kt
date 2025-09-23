package dev.solora.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.solora.quotes.QuoteDetailScreen
import dev.solora.theme.SoloraTheme

class QuoteDetailFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val id = arguments?.getLong("id") ?: 0L
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SoloraTheme {
                    val navController = findNavController()
                    QuoteDetailScreen(id = id) { navController.popBackStack() }
                }
            }
        }
    }
}
