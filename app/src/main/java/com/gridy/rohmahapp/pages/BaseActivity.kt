package com.gridy.rohmahapp.pages
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gridy.rohmahapp.R

open class BaseActivity : AppCompatActivity() {

    fun showLoading(isLoading: Boolean, message: String? = "Please wait...") {
        val layoutLoading = findViewById<View>(R.id.layout_loading)
//        val progressText = findViewById<TextView>(R.id.progressText)

        layoutLoading?.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (!message.isNullOrEmpty()) {
//            progressText?.visibility = View.VISIBLE
//            progressText?.text = message
        } else {
//            progressText?.visibility = View.GONE
        }
    }

    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }
}