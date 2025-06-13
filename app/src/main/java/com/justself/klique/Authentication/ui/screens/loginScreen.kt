package com.justself.klique.Authentication.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.justself.klique.Authentication.ui.viewModels.AuthViewModel
import com.justself.klique.Authentication.ui.viewModels.PhoneValidator
import com.justself.klique.Authentication.ui.viewModels.RegistrationStep
import com.justself.klique.CalendarBackgroundStyle
import com.justself.klique.CalendarUI
import com.justself.klique.Logger
import com.justself.klique.SessionManager
import java.time.YearMonth
import java.util.Calendar
import java.util.Locale

@Composable
fun RegistrationScreen(
    authViewModel: AuthViewModel = viewModel()
) {
    val registrationStep by authViewModel.registrationStep.collectAsState()
    val aiMessage by authViewModel.aiMessage.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.fetchAiMessageForStep(registrationStep)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = aiMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(16.dp))

            when (registrationStep) {
                RegistrationStep.PHONE_NUMBER -> PhoneNumberScreen(authViewModel)
                RegistrationStep.CONFIRMATION_CODE -> ConfirmationCodeScreen(authViewModel)
                RegistrationStep.NAME -> NameScreen(authViewModel)
                RegistrationStep.GENDER -> GenderScreen(authViewModel)
                RegistrationStep.YEAR_OF_BIRTH -> YearOfBirthScreen(authViewModel)
                RegistrationStep.COMPLETE -> RegistrationCompleteScreen(authViewModel)
            }
        }
    }
}

@Composable
fun PhoneNumberScreen(authViewModel: AuthViewModel) {
    var phoneNumber by remember { mutableStateOf("") }
    val defaultCountry = remember { SessionManager.getUserCountryCode() }
    var selectedCountry by remember { mutableStateOf(defaultCountry) }
    val phoneUtil = remember { PhoneNumberUtil.getInstance() }
    val errorMessage by authViewModel.errorMessage.collectAsState()
    var isLoading by remember {
        mutableStateOf(false)
    }
    val isEnabled = PhoneValidator.isValid(phoneNumber, selectedCountry)
    DisposableEffect(Unit) {
        onDispose {
            isLoading = false
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = Locale("", selectedCountry).displayCountry,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CountryCodePicker(
                selectedCountry = selectedCountry,
                onCountrySelected = { selectedCountry = it }
            )

            Spacer(Modifier.width(8.dp))

            TextField(
                value = phoneNumber,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        phoneNumber = newValue
                    }
                },
                label = { Text("Phone Number") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = {
                PhoneValidator.formatE164(phoneNumber, selectedCountry)
                    ?.let { formatted ->
                        authViewModel.verifyPhoneNumber(formatted, selectedCountry)
                    }
            },
            enabled = isEnabled
        ) {
            Text("Continue")
        }
    }
}

fun formatPhoneNumber(
    phoneNumber: String,
    countryCode: String,
    phoneUtil: PhoneNumberUtil
): String? {
    return try {
        val number = phoneUtil.parse(phoneNumber, countryCode)
        if (phoneUtil.isValidNumberForRegion(number, countryCode)) {
            phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun CountryCodePicker(selectedCountry: String, onCountrySelected: (String) -> Unit) {
    val countryList = Locale.getISOCountries().map { country ->
        Locale("", country).displayCountry to country
    }.sortedBy { it.first }

    var expanded by remember { mutableStateOf(false) }
    Box(
        Modifier
            .wrapContentSize()
            .background(MaterialTheme.colorScheme.onPrimary)
    ) {
        Text(
            text = "+${getCountryCodeForRegion(selectedCountry)}",
            modifier = Modifier
                .clickable { expanded = true }
                .padding(16.dp),
            color = MaterialTheme.colorScheme.background
        )

        DropdownMenu(
            expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        ) {
            countryList.forEach { (countryName, countryCode) ->
                DropdownMenuItem(
                    onClick = {
                        onCountrySelected(countryCode)
                        expanded = false
                    },
                    text = { Text(countryName, color = MaterialTheme.colorScheme.onPrimary) }
                )
            }
        }
    }
}

fun getCountryCodeForRegion(region: String): String {
    return PhoneNumberUtil.getInstance().getCountryCodeForRegion(region).toString()
}

fun isValidPhoneNumber(number: String, region: String, phoneUtil: PhoneNumberUtil): Boolean {
    return try {
        val phoneNumber: Phonenumber.PhoneNumber = phoneUtil.parse(number, region)
        phoneUtil.isValidNumber(phoneNumber)
    } catch (e: Exception) {
        false
    }
}

@Composable
fun ConfirmationCodeScreen(
    authViewModel: AuthViewModel
) {
    var confirmationCode by remember { mutableStateOf("") }
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val countdown by authViewModel.countdown.collectAsState()
    val canResendCode by authViewModel.canResendCode.collectAsState()
    LaunchedEffect(Unit) {
        authViewModel.setErrorMessageToNull()
        authViewModel.startCountdown()
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter the 6‑digit confirmation code sent to your phone",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        OutlinedTextField(
            value = confirmationCode,
            onValueChange = { newCode ->
                if (newCode.length <= 6 && newCode.all(Char::isDigit)) {
                    confirmationCode = newCode
                }
            },
            label = { Text("Confirmation Code", color = MaterialTheme.colorScheme.onPrimary) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background.copy(
                    alpha = 0.5f
                ),
                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
            )
        )

        Spacer(Modifier.height(24.dp))
        if (errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        if (!canResendCode) {
            Text(
                text = "Resend code available in $countdown s",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Button(
                onClick = { authViewModel.resendCode() },
                modifier = Modifier.padding(bottom = 8.dp)
            ) { Text("Resend Code") }
        }
        Button(
            onClick = { authViewModel.retryConfirmationCode(confirmationCode) },
            enabled = confirmationCode.length == 6
        ) { Text("Verify") }
    }
}

@Composable
fun NameScreen(authViewModel: AuthViewModel) {
    var name by remember { mutableStateOf("") }
    val isNameValid = remember(name) { name.length in 2..30 }
    val errorMessage by authViewModel.errorMessage.collectAsState()
    LaunchedEffect(Unit) {
        authViewModel.setErrorMessageToNull()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Enter your name",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TextField(
            value = name,
            onValueChange = { newName ->
                if (newName.length <= 30) {
                    name = newName
                }
            },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (isNameValid) {
                    authViewModel.verifyName(name)
                }
            },
            enabled = isNameValid
        ) {
            Text("Continue")
        }

        if (!isNameValid && name.isNotEmpty()) {
            Text(
                text = "Name must be between 2 and 30 characters.",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun GenderScreen(authViewModel: AuthViewModel) {
    var gender by remember { mutableStateOf<Gender?>(null) }
    LaunchedEffect(Unit) {
        authViewModel.setErrorMessageToNull()
    }

    Column {
        Row {
            StyledRadioButton(
                selected = gender == Gender.MALE,
                onClick = { gender = Gender.MALE },
                label = "Male"
            )
            Spacer(Modifier.width(8.dp))
            StyledRadioButton(
                selected = gender == Gender.FEMALE,
                onClick = { gender = Gender.FEMALE },
                label = "Female"
            )
            Spacer(Modifier.width(8.dp))
            StyledRadioButton(
                selected = gender == Gender.OTHER,
                onClick = { gender = Gender.OTHER },
                label = "Other"
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                authViewModel.verifyGender(gender!!)
            },
            enabled = gender != null
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun StyledRadioButton(selected: Boolean, onClick: () -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.onPrimary, // Color when selected
                unselectedColor = MaterialTheme.colorScheme.onPrimary // Color when unselected
            )
        )
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun YearOfBirthScreen(authViewModel: AuthViewModel) {
    var showCalendar by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedDay by remember { mutableIntStateOf(0) }
    var selectedMonth by remember { mutableIntStateOf(0) }
    var selectedYear by remember { mutableIntStateOf(0) }
    val errorMessage by authViewModel.errorMessage.collectAsState()
    var selectedYearMonth by remember { mutableStateOf<YearMonth?>(null) }

    LaunchedEffect(Unit) {
        authViewModel.setErrorMessageToNull()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.2f))
                .align(Alignment.Center)
        ) {
            Text(
                text = selectedDate.ifEmpty { "Select Year of Birth" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clickable { showCalendar = true }
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .fillMaxWidth()
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedDate.isNotEmpty()) {
                        authViewModel.verifyBirthday(selectedDay, selectedMonth, selectedYear)
                    }
                },
                enabled = selectedDate.isNotEmpty(),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("Continue")
            }
        }

        if (showCalendar) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showCalendar = false }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                CalendarUI(
                    onDayClick = { date ->
                        selectedYear = date.year
                        selectedMonth = date.monthValue
                        selectedDay = date.dayOfMonth
                        selectedDate = formatDateToString(
                            date.dayOfMonth,
                            date.monthValue,
                            date.year
                        )
                        selectedYearMonth = YearMonth.of(date.year, date.month)
                        showCalendar = false
                    },
                    allowFutureDates = false,
                    calendarBackground = CalendarBackgroundStyle.Background,
                    currentMonthParam = selectedYearMonth ?: YearMonth.now()
                )
            }
        }
    }
}

fun formatDateToString(day: Int, month: Int, year: Int): String {
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val daySuffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }

    return "You were born on the ${day}${daySuffix} of ${months[month]}, the year $year?"
}

@Composable
fun DatePickerDialogSample(
    onDismissRequest: () -> Unit,
    onDateChange: (Int, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateChange(year, month, dayOfMonth)
                showDialog = false
                onDismissRequest()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}

@Composable
fun RegistrationCompleteScreen(authViewModel: AuthViewModel) {
    var checkedState by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Text(
                text = EULA_TEXT,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = checkedState,
                        onCheckedChange = {
                            Logger.d("Eula", "Checkbox clicked: $it")
                            checkedState = it
                        }
                    )
                    Text("I agree to the terms")
                }

                Button(
                    onClick = { authViewModel.completeRegistration() },
                    enabled = checkedState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

enum class Gender(val nameString: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other")
}

const val EULA_TEXT = """
Welcome to Klique!

By proceeding, you agree to the following terms:

1. No Objectionable Content:
   - Klique has a zero-tolerance policy for objectionable content. Any content deemed abusive, harmful, or offensive will result in immediate account suspension or termination.

2. Respectful Behavior:
   - You agree to treat all users with respect. Harassment, bullying, or abusive behavior is strictly prohibited.

3. Compliance with Laws:
   - All activity on Klique must comply with applicable laws and regulations.

By agreeing to these terms, you help create a safe and welcoming environment for everyone.

Thank you for being part of Klique!
"""

@Preview
@Composable
fun PreviewRegistrationCompleteScreen() {
    RegistrationCompleteScreen(authViewModel = AuthViewModel())
}

@Preview
@Composable
fun PreviewRegistration() {
    RegistrationScreen()
}