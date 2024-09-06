package com.justself.klique.Authentication.ui.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import com.justself.klique.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONException
import java.util.Calendar

enum class RegistrationStep {
    PHONE_NUMBER, CONFIRMATION_CODE, NAME, GENDER, YEAR_OF_BIRTH, COMPLETE
}
class AuthViewModel : ViewModel() {
    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _customerId = MutableStateFlow<Int>(25)
    val customerId = _customerId.asStateFlow()

    private val _registrationStep = MutableStateFlow(RegistrationStep.PHONE_NUMBER)
    val registrationStep: StateFlow<RegistrationStep> = _registrationStep.asStateFlow()

    private val _aiMessage = MutableStateFlow("")
    val aiMessage: StateFlow<String> = _aiMessage

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private var retryCount = 0
    private val maxRetries = 3

    private val _tempCustomerId = MutableStateFlow<Int?>(null)
    val tempCustomerId: StateFlow<Int?> = _tempCustomerId.asStateFlow()

    private var _phoneNumber = MutableStateFlow<String?>(null)
    val phoneNumber: StateFlow<String?> = _phoneNumber.asStateFlow()

    private var _confirmationCode = MutableStateFlow<String?>(null)
    val confirmationCode: StateFlow<String?> = _confirmationCode.asStateFlow()

    private var _name = MutableStateFlow<String?>(null)
    val name: StateFlow<String?> = _name.asStateFlow()

    private var _gender = MutableStateFlow<String?>(null)
    val gender: StateFlow<String?> = _gender.asStateFlow()

    private var _birthday = MutableStateFlow<Triple<Int, Int, Int>?>(null)
    val birthday: StateFlow<Triple<Int, Int, Int>?> = _birthday.asStateFlow()

    fun completeRegistration() {
        viewModelScope.launch {
            _tempCustomerId.value?.let { customerId ->
                // If tempCustomerId is not null, update customerId and log in status
                _customerId.value = customerId
                _isLoggedIn.value = true
            } ?: run {
                // Handle error if tempCustomerId is null
                _errorMessage.value = "Registration data is missing or incomplete. Please try again."
            }
        }
    }

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
        _errorMessage.value = ""
        // Simulate a server call to verify the confirmation code
        viewModelScope.launch {
            val response = serverVerifyCode(code)
            if (response.isSuccess) {
                // If successful, proceed to the next step
                retryCount = 0
                _errorMessage.value = ""
                _confirmationCode.value = code
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
    fun verifyPhoneNumber(phoneNumber: String) {
        _errorMessage.value = ""
        viewModelScope.launch {
            try{
                val endpoint = "/verify-phone"
                val params = mapOf("phone" to phoneNumber)
                val responseString = NetworkUtils.makeRequest(endpoint, "POST", params)
                val response = simulateServerVerifyPhoneNumber(phoneNumber)//parseResponse(responseString)
                if (response.isSuccess) {
                    moveToNextStep()
                    _phoneNumber.value = phoneNumber
                    _errorMessage.value = ""
                } else {
                    _errorMessage.value = response.errorMessage
                }
            } catch (e: IOException) {
                _errorMessage.value = "An error occurred: ${e.message}"
            }
            // the code below is for simulation purposes. It's the same as the one
            // in  the try block. you can delete without remorse
            val response = simulateServerVerifyPhoneNumber(phoneNumber)//parseResponse(responseString)
            if (response.isSuccess) {
                moveToNextStep()
            } else {
                _errorMessage.value = response.errorMessage
            }
        }
    }
    private fun parseResponse(responseString: String): ServerResponse {
        return try {
            // Parse the responseString (assumed to be JSON)
            val jsonObject = JSONObject(responseString)
            val isSuccess = jsonObject.optBoolean("success", false)
            val errorMessage = jsonObject.optString("error_message", "")
            val customerId = if (isSuccess) jsonObject.optInt("customerId", -1).takeIf { it != -1} else null
            ServerResponse(isSuccess, errorMessage, customerId)
        } catch (e: JSONException) {
            // Handle parsing error, assume failure
            ServerResponse(isSuccess = false, errorMessage = "Failed to parse server response.")
        }
    }

    private fun simulateServerVerifyPhoneNumber(phoneNumber: String): ServerResponse {
        // Simulate server response for phone number verification
        return if (phoneNumber == "+441234567890") {
            ServerResponse(isSuccess = true, errorMessage = "")
        } else {
            ServerResponse(isSuccess = false, errorMessage = "Invalid phone number or rate limited. Please try again.")
        }
    }
    fun verifyName(name: String) {
        _errorMessage.value = ""
        viewModelScope.launch {
            // Simulate a server request to verify the name
            val response = simulateNameSubmission(name)
            if (response.isSuccess) {
                _name.value = name
                moveToNextStep()
            } else {
                _errorMessage.value = response.errorMessage
            }
        }
    }

    private suspend fun simulateNameSubmission(name: String): ServerResponse {
        delay(1000) // Simulate network delay
        return if (name.length in 2..30) {
            ServerResponse(isSuccess = true, errorMessage = "")
        } else {
            ServerResponse(isSuccess = false, errorMessage = "Name must be between 2 and 30 characters.")
        }
    }
    fun verifyBirthday(day: Int, month: Int, year: Int) {
        _errorMessage.value = ""
        viewModelScope.launch {
            // Simulate a server request to verify the birthday
            val response = simulateBirthdaySubmission(day, month, year)
            if (response.isSuccess) {
                _birthday.value = Triple(day, month, year)
                finalizeRegistration()
                moveToNextStep()
            } else {
                _errorMessage.value = response.errorMessage
            }
        }
    }

    private suspend fun simulateBirthdaySubmission(day: Int, month: Int, year: Int): ServerResponse {
        delay(1000) // Simulate network delay
        return if (year <= Calendar.getInstance().get(Calendar.YEAR) - 13) {
            // Assuming users must be at least 13 years old
            ServerResponse(isSuccess = true, errorMessage = "")
        } else {
            ServerResponse(isSuccess = false, errorMessage = "You must be at least 13 years old.")
        }
    }
    fun setErrorMessageToNull(){
        _errorMessage.value = ""
    }
    fun verifyGender(selectedGender: String) {
        _errorMessage.value = ""
        viewModelScope.launch {
            _gender.value = selectedGender // Store selected gender
            val response = simulateGenderSubmission(selectedGender)
            if (response.isSuccess) {
                moveToNextStep()
                _gender.value = selectedGender
                _errorMessage.value = ""
            } else {
                _errorMessage.value = response.errorMessage
            }
        }
    }
    private suspend fun simulateGenderSubmission(selectedGender: String): ServerResponse {
        delay(1000) // Simulate network delay
        return if (selectedGender in listOf("Male", "Female", "Other")) {
            ServerResponse(isSuccess = true, errorMessage = "")
        } else {
            ServerResponse(isSuccess = false, errorMessage = "Invalid gender selection.")
        }
    }
    private fun finalizeRegistration() {
        viewModelScope.launch {
            val completeRegistrationData = mutableMapOf<String, String>()

            // Safely add each non-null value to the map
            phoneNumber.value?.let { completeRegistrationData["phoneNumber"] = it }
            confirmationCode.value?.let { completeRegistrationData["confirmationCode"] = it }
            name.value?.let { completeRegistrationData["name"] = it }
            gender.value?.let { completeRegistrationData["gender"] = it }
            birthday.value?.let {
                completeRegistrationData["birthday"] = "${it.first}-${it.second}-${it.third}"
            }

            try {
                val responseString = NetworkUtils.makeRequest("/finalize-registration", "POST", completeRegistrationData)
                val response = parseResponse(responseString)

                if (response.isSuccess) {
                    response.customerId?.let {
                        _tempCustomerId.value = it
                    }
                    // Handle successful registration
                } else {
                    _errorMessage.value = response.errorMessage
                }
            } catch (e: IOException) {
                _errorMessage.value = "An error occurred: ${e.message}"
                Log.d("Final", "$completeRegistrationData")
            }
        }
    }
}
data class ServerResponse(val isSuccess: Boolean, val errorMessage: String, val customerId: Int? = null)