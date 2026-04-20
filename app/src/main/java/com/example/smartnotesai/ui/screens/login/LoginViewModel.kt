package com.example.smartnotesai.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.smartnotesai.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val errorMessage: String? = null,
    val loginSucceeded: Boolean = false
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.update { current ->
            current.copy(email = email, errorMessage = null, loginSucceeded = false)
        }
    }

    fun updatePassword(password: String) {
        _uiState.update { current ->
            current.copy(password = password, errorMessage = null, loginSucceeded = false)
        }
    }

    fun submit() {
        val current = _uiState.value
        if (!authRepository.login(current.email, current.password)) {
            _uiState.update { state ->
                state.copy(errorMessage = "Enter any email and password to continue.")
            }
            return
        }

        _uiState.update { state ->
            state.copy(errorMessage = null, loginSucceeded = true)
        }
    }

    fun consumeLoginSuccess() {
        _uiState.update { state ->
            state.copy(loginSucceeded = false)
        }
    }
}
