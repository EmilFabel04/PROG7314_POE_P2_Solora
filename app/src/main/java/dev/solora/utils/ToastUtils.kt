package dev.solora.utils

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import dev.solora.R

object ToastUtils {
    
    /**
     * Show a custom orange-themed toast
     */
    fun showOrangeToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)
        
        val textView = layout.findViewById<TextView>(R.id.toast_text)
        textView.text = message
        
        val toast = Toast(context)
        toast.duration = duration
        toast.view = layout
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
        toast.show()
    }
    
    /**
     * Show a custom orange-themed toast with custom gravity
     */
    fun showOrangeToast(
        context: Context, 
        message: String, 
        gravity: Int = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)
        
        val textView = layout.findViewById<TextView>(R.id.toast_text)
        textView.text = message
        
        val toast = Toast(context)
        toast.duration = duration
        toast.view = layout
        toast.setGravity(gravity, 0, 100)
        toast.show()
    }
}
