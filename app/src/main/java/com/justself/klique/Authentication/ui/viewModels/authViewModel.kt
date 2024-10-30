package com.justself.klique.Authentication.ui.viewModels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.util.appendPlaceholders
import com.justself.klique.Authentication.ui.screens.Gender
import com.justself.klique.KliqueHttpMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import com.justself.klique.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.json.JSONException
import java.util.Calendar

enum class RegistrationStep {
    PHONE_NUMBER, CONFIRMATION_CODE, NAME, GENDER, YEAR_OF_BIRTH, COMPLETE
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private val sharedPreferences = context.getSharedPreferences("KliqueApp", Context.MODE_PRIVATE)
    private val savedCustomerId = sharedPreferences.getInt("customerId", -1)

    private val _customerId = MutableStateFlow(1)
    val customerId = _customerId.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = _customerId.map { it > 0 }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        _customerId.value > 0
    )

    private val _registrationStep = MutableStateFlow(RegistrationStep.PHONE_NUMBER)
    val registrationStep: StateFlow<RegistrationStep> = _registrationStep.asStateFlow()

    private val _aiMessage = MutableStateFlow("")
    val aiMessage: StateFlow<String> = _aiMessage

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private var retryCount = 0
    private val maxRetries = 5

    private val _tempCustomerId = MutableStateFlow<Int?>(null)
    val tempCustomerId: StateFlow<Int?> = _tempCustomerId.asStateFlow()

    private var _phoneNumber = MutableStateFlow<String?>(null)
    private val phoneNumber: StateFlow<String?> = _phoneNumber.asStateFlow()

    private var _confirmationCode = MutableStateFlow<String?>(null)
    private val confirmationCode: StateFlow<String?> = _confirmationCode.asStateFlow()

    private var _name = MutableStateFlow<String?>(null)
    val name: StateFlow<String?> = _name.asStateFlow()

    private var _gender = MutableStateFlow<String?>(null)
    private val gender: StateFlow<String?> = _gender.asStateFlow()

    private var _birthday = MutableStateFlow<Triple<Int, Int, Int>?>(null)
    private val birthday: StateFlow<Triple<Int, Int, Int>?> = _birthday.asStateFlow()
    private val countdownSeconds = 30

    private val _countdown = MutableStateFlow(countdownSeconds)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _canResendCode = MutableStateFlow(false)
    val canResendCode: StateFlow<Boolean> = _canResendCode.asStateFlow()
    private var isServerAIMessage = false

    fun fetchAiMessageForStep(step: RegistrationStep) {
        if (isServerAIMessage) {
            isServerAIMessage = false
            return
        }
        val message = when (step) {
            RegistrationStep.PHONE_NUMBER -> "Welcome to Klique, Please enter your phone number."
            RegistrationStep.CONFIRMATION_CODE -> "We've sent you a confirmation code. Please enter it below."
            RegistrationStep.NAME -> "Now, could you provide your name?"
            RegistrationStep.GENDER -> "Ok, ${_name.value}. What is your gender?"
            RegistrationStep.YEAR_OF_BIRTH -> "Finally, what's your year of birth?"
            RegistrationStep.COMPLETE -> "Thank you for registering!"
        }
        _aiMessage.update { message }
    }

    private fun moveToNextStep() {
        _registrationStep.update {
            when (it) {
                RegistrationStep.PHONE_NUMBER -> RegistrationStep.CONFIRMATION_CODE
                RegistrationStep.CONFIRMATION_CODE -> RegistrationStep.NAME
                RegistrationStep.NAME -> RegistrationStep.GENDER
                RegistrationStep.GENDER -> RegistrationStep.YEAR_OF_BIRTH
                RegistrationStep.YEAR_OF_BIRTH -> RegistrationStep.COMPLETE
                RegistrationStep.COMPLETE -> RegistrationStep.COMPLETE
            }
        }
        fetchAiMessageForStep(_registrationStep.value)
    }

    fun retryConfirmationCode(code: String) {
        _errorMessage.value = ""
        val jsonBody = JSONObject().put("confirmationCode", code).toString()
        viewModelScope.launch {
            try {
                val response = NetworkUtils.makeRequest(
                    "confirmationCode",
                    KliqueHttpMethod.POST,
                    emptyMap(),
                    jsonBody = jsonBody
                )
                if (response.first) {
                    retryCount = 0
                    _errorMessage.value = ""
                    _confirmationCode.value = code
                    moveToNextStep()
                } else {
                    retryCount++
                    _errorMessage.value = response.second

                    if (retryCount >= maxRetries) {
                        _errorMessage.value = "Maximum retries reached. Please try again later."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
            try {
                val endpoint = "verifyPhone"
                val phoneJson = JSONObject().apply {
                    put("number", phoneNumber)
                }.toString()
                val responseString = NetworkUtils.makeRequest(
                    endpoint,
                    KliqueHttpMethod.POST,
                    emptyMap(),
                    jsonBody = phoneJson
                )
                if (responseString.first) {
                    _aiMessage.value = responseString.second
                    isServerAIMessage = true
                    moveToNextStep()
                    _phoneNumber.value = phoneNumber
                    _errorMessage.value = ""
                } else {
                    _errorMessage.value = responseString.second
                }
            } catch (e: IOException) {
                _errorMessage.value = "An error occurred: ${e.message}"
            }
        }
    }

    private fun parseResponse(responseString: String): ServerResponse {
        return try {
            val jsonObject = JSONObject(responseString)
            val isSuccess = jsonObject.optBoolean("success", false)
            val errorMessage = jsonObject.optString("message", "")
            val customerId =
                if (isSuccess) jsonObject.optInt("userId", -1).takeIf { it != -1 } else null
            ServerResponse(isSuccess, errorMessage, customerId)
        } catch (e: JSONException) {
            ServerResponse(isSuccess = false, errorMessage = "Failed to parse server response.")
        }
    }

    private fun simulateServerVerifyPhoneNumber(phoneNumber: String): ServerResponse {
        // Simulate server response for phone number verification
        return if (phoneNumber == "+441234567890") {
            ServerResponse(isSuccess = true, errorMessage = "")
        } else {
            ServerResponse(
                isSuccess = false,
                errorMessage = "Invalid phone number or rate limited. Please try again."
            )
        }
    }

    fun verifyName(name: String) {
        _errorMessage.value = ""
        try {
            viewModelScope.launch {
                val jsonBody = JSONObject().put("name", name).toString()
                val response = NetworkUtils.makeRequest(
                    "nameSubmission",
                    KliqueHttpMethod.POST,
                    emptyMap(),
                    jsonBody = jsonBody
                )
                if (response.first) {
                    _name.value = name
                    moveToNextStep()
                } else {
                    _errorMessage.value = response.second
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun simulateNameSubmission(name: String): ServerResponse {
        delay(1000) // Simulate network delay
        return if (name.length in 2..30) {
            ServerResponse(isSuccess = true, errorMessage = "")
        } else {
            ServerResponse(
                isSuccess = false,
                errorMessage = "Name must be between 2 and 30 characters."
            )
        }
    }

    fun verifyBirthday(day: Int, month: Int, year: Int) {
        _errorMessage.value = ""
        viewModelScope.launch {
            val response = screenBirthdayAgeSubmission(day, month, year)
            if (response.isSuccess) {
                _birthday.value = Triple(day, month, year)
                finalizeRegistration()
                moveToNextStep()
            } else {
                _errorMessage.value = response.errorMessage
            }
        }
    }

    private fun screenBirthdayAgeSubmission(day: Int, month: Int, year: Int): ServerResponse {
        return if (year <= Calendar.getInstance().get(Calendar.YEAR) - 13) {
            ServerResponse(isSuccess = true, errorMessage = "")
        } else {
            ServerResponse(isSuccess = false, errorMessage = "You must be at least 13 years old.")
        }
    }

    fun setErrorMessageToNull() {
        _errorMessage.value = ""
    }

    fun verifyGender(selectedGender: Gender) {
        _errorMessage.value = ""
        viewModelScope.launch {
            _gender.value = selectedGender.nameString
            moveToNextStep()
            _gender.value = selectedGender.nameString
            _errorMessage.value = ""
        }
    }

    private fun finalizeRegistration() {
        viewModelScope.launch {
            val completeRegistrationData = JSONObject()
            phoneNumber.value?.let { completeRegistrationData.put("phoneNumber", it) }
            confirmationCode.value?.let { completeRegistrationData.put("confirmationCode", it) }
            name.value?.let { completeRegistrationData.put("name", it) }
            gender.value?.let { completeRegistrationData.put("gender", it) }
            birthday.value?.let {
                val formattedBirthday = "${it.first}-${it.second}-${it.third}"
                completeRegistrationData.put("birthday", formattedBirthday)
            }
            val jsonString = completeRegistrationData.toString()
            try {
                val responseString = NetworkUtils.makeRequest(
                    "finalizeRegistration",
                    KliqueHttpMethod.POST,
                    emptyMap(),
                    jsonBody = jsonString
                )
                val response = parseResponse(responseString.second)
                if (response.isSuccess) {
                    response.customerId?.let {
                        _tempCustomerId.value = it
                        saveCustomerIdToSharedPreferences(it)
                    }
                } else {
                    _errorMessage.value = response.errorMessage
                }
            } catch (e: IOException) {
                _errorMessage.value = "An error occurred: ${e.message}"
                Log.d("Final", "$completeRegistrationData")
            }
        }
    }

    fun startCountdown() {
        _canResendCode.value = false
        viewModelScope.launch {
            while (_countdown.value > 0) {
                delay(1000L)
                _countdown.value -= 1
            }
            _canResendCode.value = true
        }
    }

    private fun resetCountdown() {
        _countdown.value = countdownSeconds
        startCountdown()
    }

    fun resendCode() {
        _phoneNumber.value?.let { phoneNumber ->
            verifyPhoneNumber(phoneNumber)
            resetCountdown()
        }
    }

    private fun saveCustomerIdToSharedPreferences(customerId: Int) {
        val sharedPreferences = context.getSharedPreferences("KliqueApp", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("customerId", customerId)
        editor.apply()
    }

    fun completeRegistration() {
        viewModelScope.launch {
            _tempCustomerId.value?.let { customerId ->
                _customerId.value = customerId
            } ?: run {
                _errorMessage.value =
                    "Registration data is missing or incomplete. Please try again."
            }
        }
    }
}

data class ServerResponse(
    val isSuccess: Boolean,
    val errorMessage: String,
    val customerId: Int? = null
)

class ViewModelProviderFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}