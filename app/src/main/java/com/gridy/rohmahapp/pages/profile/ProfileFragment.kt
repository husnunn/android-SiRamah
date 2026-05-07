// File: /app/src/main/java/com/gridy/rohmahapp/pages/profile/ProfileFragment.kt
package com.gridy.rohmahapp.pages.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.lifecycle.Observer
import com.google.android.material.textfield.TextInputLayout
import com.gridy.rohmahapp.R
import com.gridy.rohmahapp.data.model.UpdatePasswordRequest
import com.gridy.rohmahapp.data.state.UiState
import com.gridy.rohmahapp.databinding.DialogChangePasswordBinding
import com.gridy.rohmahapp.databinding.FragmentProfileBinding
import com.gridy.rohmahapp.di.Injection
import com.gridy.rohmahapp.pages.BaseFragment
import com.gridy.rohmahapp.pages.auth.LoginActivity
import com.gridy.rohmahapp.utils.PreferenceClass
import com.gridy.rohmahapp.utils.ProfilePhotoUpload
import com.gridy.rohmahapp.viewmodel.AuthViewModel
import com.gridy.rohmahapp.viewmodel.ProfileViewModel
import com.gridy.rohmahapp.viewmodel.factory.AuthViewModelFactory
import com.gridy.rohmahapp.viewmodel.factory.ProfileViewModelFactory

class ProfileFragment : BaseFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            val part = ProfilePhotoUpload.buildPhotoPart(requireContext(), uri)
            profileViewModel.uploadProfilePhoto(part)
        }.onFailure { e ->
            showToast(e.message ?: getString(R.string.erha_popup_title_error))
        }
    }

    override fun isSwipeRefreshEnabled(): Boolean = true

    override fun onRefreshData() {
        beginTrackedSwipeRefresh(1)
        profileViewModel.loadProfile(manualRefresh = true)
    }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            Injection.provideAuthRepository(requireContext()),
        )
    }

    private val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(
            Injection.provideProfileRepository(requireContext()),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSwipeRefresh(binding.swipeRefresh)
        setupClickListener()
        observeProfile()
        observeUploadPhoto()
        observeLogout()
        profileViewModel.loadProfile()
    }

    private fun setupClickListener() {
        binding.frameProfilePhoto.setOnClickListener {
            pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }

        binding.itemChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.itemNotification.setOnClickListener {
            showToast("Menu Notifikasi")
        }

        binding.itemAbout.setOnClickListener {
            showToast("Menu Tentang Aplikasi")
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun observeProfile() {
        profileViewModel.profileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> showLoading(false)
                is UiState.Loading -> showLoading(true)

                is UiState.Success -> {
                    showLoading(false)
                    val data = state.data
                    val role = PreferenceClass(requireContext()).getString(PreferenceClass.KEY_USER_ROLE)

                    binding.tvUserName.text = data.name
                    binding.tvUserRole.text = data.roleLabel
                    binding.tvEmail.text = data.email
                    binding.tvRoleType.text = data.roleTypeLabel ?: "—"
                    binding.tvUserId.text = data.userIdLabel

                    binding.tvSchool.text = data.schoolName?.takeIf { it.isNotBlank() } ?: "—"
                    binding.rowSchool.isVisible = true
                    binding.dividerAfterSchool.isVisible = true

                    if (role == "student") {
                        binding.rowClass.isVisible = true
                        binding.dividerAfterClass.isVisible = true
                        binding.tvClass.text = data.className?.takeIf { it.isNotBlank() } ?: "—"
                    } else {
                        binding.rowClass.isVisible = false
                        binding.dividerAfterClass.isVisible = false
                    }

                    binding.tvIdentityCaption.text = if (role == "teacher") {
                        getString(R.string.profile_identity_nip)
                    } else {
                        getString(R.string.profile_identity_nis)
                    }

                    bindProfilePhoto(data.photoUrl)
                    swipeRefreshStepDone()
                }

                is UiState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                    swipeRefreshStepDone()
                }
            }
        }
    }

    private fun bindProfilePhoto(url: String?) {
        val iv = binding.ivProfilePhoto
        if (url.isNullOrBlank()) {
            Glide.with(this).clear(iv)
            iv.setImageResource(android.R.drawable.ic_menu_myplaces)
            iv.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.on_brand)
        } else {
            iv.imageTintList = null
            Glide.with(this)
                .load(url)
                .circleCrop()
                .into(iv)
        }
    }

    private fun observeUploadPhoto() {
        profileViewModel.uploadPhotoState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> Unit
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> {
                    showLoading(false)
                    showToast(state.data)
                    profileViewModel.resetUploadPhotoState()
                }
                is UiState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                    profileViewModel.resetUploadPhotoState()
                }
            }
        }
    }

    private fun showChangePasswordDialog() {
        profileViewModel.resetPasswordState()
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        var dialogRef: AlertDialog? = null

        val observer = Observer<UiState<Unit>> { state ->
            val dialog = dialogRef ?: return@Observer
            when (state) {
                is UiState.Idle -> Unit
                is UiState.Loading -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                }
                is UiState.Success -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                    showToast(getString(R.string.erha_popup_title_success) + " Password diperbarui.")
                    dialog.dismiss()
                }
                is UiState.Error -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                    applyPasswordFieldErrors(
                        dialogBinding.tilCurrentPassword,
                        dialogBinding.tilNewPassword,
                        dialogBinding.tilConfirmPassword,
                        state.fieldErrors,
                        state.message,
                    )
                    profileViewModel.resetPasswordState()
                }
            }
        }

        val dlg = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_change_password_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.profile_change_password_save, null)
            .setNegativeButton(R.string.erha_popup_confirm_cancel) { d, _ -> d.dismiss() }
            .create()

        dialogRef = dlg

        dlg.setOnShowListener {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                clearPasswordFieldErrors(dialogBinding)
                val current = dialogBinding.etCurrentPassword.text?.toString().orEmpty()
                val newPw = dialogBinding.etNewPassword.text?.toString().orEmpty()
                val confirm = dialogBinding.etConfirmPassword.text?.toString().orEmpty()

                if (current.isBlank() || newPw.isBlank() || confirm.isBlank()) {
                    dialogBinding.tilCurrentPassword.error =
                        getString(R.string.profile_password_validation_required)
                    return@setOnClickListener
                }
                if (newPw.length < 8) {
                    dialogBinding.tilNewPassword.error =
                        getString(R.string.profile_password_validation_length)
                    return@setOnClickListener
                }
                if (newPw != confirm) {
                    dialogBinding.tilConfirmPassword.error =
                        getString(R.string.profile_password_validation_match)
                    return@setOnClickListener
                }

                profileViewModel.updatePassword(
                    UpdatePasswordRequest(
                        current_password = current,
                        password = newPw,
                        password_confirmation = confirm,
                    ),
                )
            }
        }

        dlg.setOnDismissListener {
            profileViewModel.passwordState.removeObserver(observer)
            profileViewModel.resetPasswordState()
        }

        profileViewModel.passwordState.observe(viewLifecycleOwner, observer)
        dlg.show()
    }

    private fun clearPasswordFieldErrors(b: DialogChangePasswordBinding) {
        b.tilCurrentPassword.error = null
        b.tilNewPassword.error = null
        b.tilConfirmPassword.error = null
    }

    private fun applyPasswordFieldErrors(
        tilCurrent: TextInputLayout,
        tilNew: TextInputLayout,
        tilConfirm: TextInputLayout,
        fieldErrors: Map<String, Any>,
        fallbackMessage: String,
    ) {
        val cur = firstFieldMessage(fieldErrors, "current_password")
        val newErr = firstFieldMessage(fieldErrors, "password")
        val conf = firstFieldMessage(fieldErrors, "password_confirmation")
        if (cur != null) tilCurrent.error = cur
        if (newErr != null) tilNew.error = newErr
        if (conf != null) tilConfirm.error = conf
        if (cur == null && newErr == null && conf == null) {
            showToast(fallbackMessage)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun firstFieldMessage(fieldErrors: Map<String, Any>, key: String): String? {
        val v = fieldErrors[key] ?: return null
        return when (v) {
            is List<*> -> v.firstOrNull()?.toString()
            is String -> v
            else -> null
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah Anda yakin ingin keluar dari akun ini?")
            .setPositiveButton("Ya") { dialog, _ ->
                dialog.dismiss()
                authViewModel.logout()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun observeLogout() {
        authViewModel.logoutState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> Unit
                is UiState.Loading -> showLoading(true)

                is UiState.Success -> {
                    showLoading(false)
                    showToast(state.data)

                    val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }

                is UiState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
