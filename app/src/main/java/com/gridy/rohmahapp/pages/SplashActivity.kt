package com.gridy.rohmahapp.pages


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gridy.rohmahapp.R
import com.gridy.rohmahapp.databinding.ActivitySplashBinding
import com.gridy.rohmahapp.pages.auth.LoginActivity
import com.gridy.rohmahapp.utils.PreferenceClass

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

//    @Inject
//    lateinit var viewModelFactory: com.gridy.erhadev.di.ViewModelFactory<AuthViewModel>
//    private val viewModel: AuthViewModel by viewModels { viewModelFactory }

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        component.inject(this)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Handler(Looper.getMainLooper()).postDelayed({
            checkSession()
//            startActivity(Intent(this, LoginActivity::class.java))
        }, 2000)
    }

    private fun checkSession() {
        val pref = PreferenceClass(this)
        val token = pref.getString(PreferenceClass.KEY_USER_TOKEN)

        if (token.isNotBlank()) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}

