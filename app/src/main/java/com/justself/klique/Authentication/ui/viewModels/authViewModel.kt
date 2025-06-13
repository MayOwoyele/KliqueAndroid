package com.justself.klique.Authentication.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.justself.klique.Authentication.ui.screens.Gender
import com.justself.klique.DroidAppUpdateManager
import com.justself.klique.JWTNetworkCaller
import com.justself.klique.KliqueHttpMethod
import com.justself.klique.Logger
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.NetworkUtils
import com.justself.klique.SessionManager
import com.justself.klique.SessionManager.saveCountryToSharedPreferences
import com.justself.klique.SessionManager.saveCustomerIdToSharedPreferences
import com.justself.klique.SessionManager.saveNameToSharedPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar


enum class RegistrationStep {
    PHONE_NUMBER, CONFIRMATION_CODE, NAME, GENDER, YEAR_OF_BIRTH, COMPLETE
}

enum class AppState {
    LoggedIn,
    LoggedOut,
//    UpdateRequired,
    Loading
}

class AuthViewModel : ViewModel() {
    val appState: StateFlow<AppState> =
        SessionManager.customerId
            .map { id ->
                if (id > 0) AppState.LoggedIn
                else        AppState.LoggedOut
            }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.Lazily,
                initialValue = AppState.Loading
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
    private val _tempName = MutableStateFlow<String?>(null)

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
    private val countdownSeconds = 60

    private val _countdown = MutableStateFlow(countdownSeconds)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private var _country = MutableStateFlow<String?>(null)
    val country: StateFlow<String?> = _country.asStateFlow()

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
        val jsonBody = JSONObject().apply {
            put("code", code)
            put("phoneNumber", phoneNumber.value)
        }.toString()
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
                Logger.d("KliqueCode", e.toString())
            }
        }
    }

    fun verifyPhoneNumber(phoneNumber: String, selectedCountry: String, retrial: Boolean = false) {
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
                    if (!retrial) {
                        moveToNextStep()
                    }
                    _phoneNumber.value = phoneNumber
                    _country.value = selectedCountry
                    _errorMessage.value = ""
                } else {
                    _errorMessage.value = responseString.second
                }
            } catch (e: IOException) {
                Logger.d("SignUpError", e.toString())
                _errorMessage.value =
                    "I'm sorry, something went wrong and we do not know. Please try again soon"
            }
        }
    }

    private fun parseResponse(responseString: String): SignUpServerResponse {
        return try {
            val jsonObject = JSONObject(responseString)
            val isSuccess = jsonObject.getBoolean("success")
            val errorMessage = jsonObject.optString("message", "")
            val customerId =
                if (isSuccess) jsonObject.optInt("userId", -1).takeIf { it != -1 } else null
            val accessToken = jsonObject.optString("accessToken")
            val refreshToken = jsonObject.optString("refreshToken")
            val name = jsonObject.optString("name")
            SignUpServerResponse(
                isSuccess,
                errorMessage,
                customerId,
                refreshToken,
                accessToken,
                name
            )
        } catch (e: JSONException) {
            SignUpServerResponse(
                isSuccess = false,
                errorMessage = "Failed to parse server response"
            )
        }
    }

    fun verifyName(name: String) {
        _errorMessage.value = ""
        viewModelScope.launch {
            try {
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
            } catch (e: Exception) {
                Logger.d("KliqueName", e.toString())
            }
        }
    }

    fun verifyBirthday(day: Int, month: Int, year: Int) {
        _errorMessage.value = ""
        viewModelScope.launch {
            val response = screenBirthdayAgeSubmission(year)
            if (response.isSuccess) {
                _birthday.value = Triple(day, month, year)
                if (finalizeRegistration()) {
                    moveToNextStep()
                } else {
                    _errorMessage.value = "There was an error signing up. Please try again soon"
                }
            } else {
                _errorMessage.value = response.errorMessage
            }
        }
    }

    private fun screenBirthdayAgeSubmission(year: Int): ServerResponse {
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
            _errorMessage.value = ""
        }
    }

    private suspend fun finalizeRegistration(): Boolean {
        val completeRegistrationData = JSONObject()
        phoneNumber.value?.let { completeRegistrationData.put("phoneNumber", it) }
        confirmationCode.value?.let { completeRegistrationData.put("confirmationCode", it) }
        name.value?.let { completeRegistrationData.put("name", it) }
        gender.value?.let { completeRegistrationData.put("gender", it) }
        birthday.value?.let {
            val (day, month, year) = birthday.value ?: Triple(1, 1, 1970)
            val date = LocalDate.of(year, month, day)
            val formatted = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            completeRegistrationData.put("birthday", formatted)
        }
        completeRegistrationData.put("platform", "android")
        country.value?.let { completeRegistrationData.put("country", it) }
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
                    _tempName.value = response.name
                    JWTNetworkCaller.saveTokens(
                        appContext,
                        response.accessToken!!,
                        response.refreshToken!!
                    )
                    saveCountryToSharedPreferences(country.value!!)
                    Logger.d("refreshToken", "During login, ${response.refreshToken}")
                }
                return true
            } else {
                response.errorMessage?.let {
                    _errorMessage.value = it
                }
            }
        } catch (e: IOException) {
            _errorMessage.value =
                "An error occurred and we are not sure what it is. Please try again soon"
            Logger.d("Final", "$completeRegistrationData")
        }
        return false
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
            verifyPhoneNumber(phoneNumber, country.value!!, true)
            resetCountdown()
        }
    }

    fun completeRegistration() {
        viewModelScope.launch {
            _tempCustomerId.value?.let {
                saveCustomerIdToSharedPreferences(it)
                _tempName.value?.let { it1 -> saveNameToSharedPreferences(it1) }
                SessionManager.fetchCustomerDataFromSharedPreferences()
                _registrationStep.value = RegistrationStep.PHONE_NUMBER
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

data class SignUpServerResponse(
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val customerId: Int? = null,
    val refreshToken: String? = null,
    val accessToken: String? = null,
    val name: String? = null
)
object PhoneValidator {
    private val util = PhoneNumberUtil.getInstance()

    /**
     * Returns the E.164‐formatted number if it’s valid for [regionCode], else null.
     * Handles inputs like "8031234567", "08031234567" or full "+2348031234567".
     */
    fun formatE164(rawInput: String, regionCode: String): String? {
        // 1️⃣ Strip everything except digits or leading '+'
        val cleaned = rawInput.filter { it.isDigit() || it == '+' }

        // 2️⃣ Parse: if we see a '+', let libphonenumber infer country;
        //    otherwise parse as a national number in [regionCode].
        val parsed = try {
            if (cleaned.startsWith('+')) util.parse(cleaned, null)
            else                      util.parse(cleaned, regionCode)
        } catch (_: NumberParseException) {
            return null
        }

        // 3️⃣ Strict check: number must both be valid and match [regionCode]
        return if (util.isValidNumberForRegion(parsed, regionCode)) {
            util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
        } else {
            null
        }
    }

    /**
     * Just a boolean wrapper around [formatE164].
     */
    fun isValid(rawInput: String, regionCode: String): Boolean =
        formatE164(rawInput, regionCode) != null
}