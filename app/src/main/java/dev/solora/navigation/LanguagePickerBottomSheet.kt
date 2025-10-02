package dev.solora.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.solora.R

class LanguagePickerBottomSheet(private val onSelected: (String) -> Unit) : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.sheet_language_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val options = listOf(
            R.id.lang_en to "English",
            R.id.lang_af to "Afrikaans",
            R.id.lang_xh to "isiXhosa"
        )
        options.forEach { (id, label) ->
            view.findViewById<TextView>(id)?.setOnClickListener {
                onSelected(label)
                dismiss()
            }
        }
    }
}


