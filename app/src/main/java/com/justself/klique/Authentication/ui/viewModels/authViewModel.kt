package com.justself.klique.Authentication.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import android.util.Log  // Import Android Log utility
import com.justself.klique.NetworkUtils
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class RegistrationStep {
    PHONE_NUMBER, CONFIRMATION_CODE, NAME, GENDER, YEAR_OF_BIRTH, COMPLETE
}
class AuthViewModel : ViewModel() {
    private val _registrationStep = MutableStateFlow(RegistrationStep.PHONE_NUMBER)
    val registrationStep: StateFlow<RegistrationStep> = _registrationStep.asStateFlow()

    private val _aiMessage = MutableStateFlow("")
    val aiMessage: StateFlow<String> = _aiMessage

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private var retryCount = 0
    private val maxRetries = 3

    fun fetchAiMessageForStep(step: RegistrationStep) {
        // Simulate a network call to get the AI message for the current step
        val message = when (step) {
            RegistrationStep.PHONE_NUMBER -> "Welcome to Klique, Please enter your phone number."
            RegistrationStep.CONFIRMATION_CODE -> "We've sent you a confirmation code. Please enter it below."
            RegistrationStep.NAME -> "Now, could you provide your name?"
            RegistrationStep.GENDER -> "What is your gender?"
            RegistrationStep.YEAR_OF_BIRTH -> "Finally, what's your year of birth?"
            RegistrationStep.COMPLETE -> "Thank you for registering!"
        }
        _aiMessage.update { message }
    }
    fun moveToNextStep() {
        _registrationStep.update {
            when (it) {
                RegistrationStep.PHONE_NUMBER -> RegistrationStep.CONFIRMATION_CODE
                RegistrationStep.CONFIRMATION_CODE -> RegistrationStep.NAME
                RegistrationStep.NAME -> RegistrationStep.GENDER
                RegistrationStep.GENDER -> RegistrationStep.YEAR_OF_BIRTH
                RegistrationStep.YEAR_OF_BIRTH -> RegistrationStep.COMPLETE
                RegistrationStep.COMPLETE -> RegistrationStep.COMPLETE // No further step
            }
        }
        fetchAiMessageForStep(_registrationStep.value) // Fetch the message for the new step
    }
    fun retryConfirmationCode(code: String) {
        // Simulate a server call to verify the confirmation code
        viewModelScope.launch {
            val response = serverVerifyCode(code)
            if (response.isSuccess) {
                // If successful, proceed to the next step
                retryCount = 0
                moveToNextStep()
            } else {
                // Increment retry count and update error message from server response
                retryCount++
                _errorMessage.value = response.errorMessage

                if (retryCount >= maxRetries) {
                    _errorMessage.value = "Maximum retries reached. Please try again later."
                    // Optionally, handle logic for banning or retrying after a cooldown
                }
            }
        }
    }
    private fun serverVerifyCode(code: String): ServerResponse {
        return if (code == "123456") {
            ServerResponse(isSuccess = true, errorMessage = "")
        } else {
            ServerResponse(isSuccess = false, errorMessage = "Incorrect code, try again")
        }
    }

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _customerId = MutableStateFlow<Int>(25)
    val customerId = _customerId.asStateFlow()
}
data class ServerResponse(val isSuccess: Boolean, val errorMessage: String)