package com.gridy.rohmahapp.pages.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.gridy.rohmahapp.di.Injection
import com.gridy.rohmahapp.data.state.UiState
import com.gridy.rohmahapp.databinding.ActivityLoginBinding
import com.gridy.rohmahapp.pages.BaseActivity
import com.gridy.rohmahapp.pages.MainActivity
import com.gridy.rohmahapp.ui.popup.ErHaStandardStyle
import com.gridy.rohmahapp.ui.popup.showErHaFeedback
import com.gridy.rohmahapp.viewmodel.AuthViewModel
import com.gridy.rohmahapp.viewmodel.factory.AuthViewModelFactory

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            Injection.provideAuthRepository(this),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClick()
        observeLogin()
    }

    private fun setupClick() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isBlank()) {
                showToast("Username wajib diisi")
                return@setOnClickListener
            }

            if (password.isBlank()) {
                showToast("Password wajib diisi")
                return@setOnClickListener
            }

            when (binding.rgRole.checkedRadioButtonId) {
                binding.rbStudent.id -> {
                    viewModel.loginStudent(username, password)
                }

                binding.rbTeacher.id -> {
                    viewModel.loginTeacher(username, password)
                }

                else -> {
                    showToast("Pilih role terlebih dahulu")
                }
            }
        }
    }

    private fun observeLogin() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is UiState.Idle -> {
                    showLoading(false)
                }

                is UiState.Loading -> {
                    showLoading(true)
                }

                is UiState.Success -> {
                    showLoading(false)
                    showToast(state.data.message)

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }

                is UiState.Error -> {
                    showLoading(false)
                    showErHaFeedback(state.message, ErHaStandardStyle.ERROR)
                }
            }
        }
    }
}