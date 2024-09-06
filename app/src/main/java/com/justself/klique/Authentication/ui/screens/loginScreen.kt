package com.justself.klique.Authentication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.justself.klique.Authentication.ui.viewModels.AuthViewModel
import com.justself.klique.Authentication.ui.viewModels.RegistrationStep
import java.util.Calendar
import java.util.Locale


@Composable
fun RegistrationScreen(
    navController: NavHostController,
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
            // AI Message Display
            Text(
                text = aiMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(16.dp))

            // Render appropriate screen content based on registration step
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
    var selectedCountry by remember { mutableStateOf("GB") }
    val countryCode = remember(selectedCountry) { getCountryCodeForRegion(selectedCountry) }
    val phoneUtil = remember { PhoneNumberUtil.getInstance() }
    val errorMessage by authViewModel.errorMessage.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        // Display Country Name
        Text(
            text = Locale("", selectedCountry).displayCountry,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Country Code and Phone Number Input
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
        if (errorMessage.isNotEmpty()){
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Continue Button
        Button(
            onClick = {
                if (isValidPhoneNumber(phoneNumber, selectedCountry, phoneUtil)) {
                    authViewModel.verifyPhoneNumber("+$countryCode$phoneNumber")
                }
            },
            enabled = phoneNumber.isNotEmpty() && isValidPhoneNumber(
                "+$countryCode$phoneNumber",
                selectedCountry,
                phoneUtil
            )
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun CountryCodePicker(selectedCountry: String, onCountrySelected: (String) -> Unit) {
    val countryList = Locale.getISOCountries().map { country ->
        Locale("", country).displayCountry to country
    }.sortedBy { it.first }

    var expanded by remember { mutableStateOf(false) }

    // Display the country code as a clickable text
    Box {
        Text(
            text = "+${getCountryCodeForRegion(selectedCountry)}",
            modifier = Modifier
                .clickable { expanded = true } // Open the dropdown on click
                .padding(16.dp)
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
fun ConfirmationCodeScreen(authViewModel: AuthViewModel) {
    var confirmationCode by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var activeCell by remember { mutableStateOf(0) }
    val errorMessage by authViewModel.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LaunchedEffect(Unit) {
            authViewModel.setErrorMessageToNull()
        }
        Text(
            text = "Enter the confirmation code sent to your phone",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Display confirmation code in separate boxes
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(6) { index ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (activeCell == index) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f) else
                                MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                        )
                        .padding(8.dp)
                        .clickable {
                            activeCell = index
                            focusRequester.requestFocus()
                        }, // Request focus on click
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = confirmationCode.getOrNull(index)?.toString() ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        // Invisible TextField to capture input
        TextField(
            value = confirmationCode,
            onValueChange = { newCode ->
                if (newCode.length <= 6 && newCode.all { it.isDigit() }) { // Only digits allowed
                    confirmationCode = newCode
                    activeCell = newCode.length
                }
            },
            label = { Text("Confirmation Code") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester) // Set focus requester
                .alpha(0f), // Hide TextField UI
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                if (confirmationCode.length == 6) {
                    // Add logic here to verify the code
                    authViewModel.retryConfirmationCode(confirmationCode)
                }
            },
            enabled = confirmationCode.length == 6 // Enable button only when 6 digits are entered
        ) {
            Text("Verify")
        }
    }
}

@Composable
fun NameScreen(authViewModel: AuthViewModel) {
    var name by remember { mutableStateOf("") }
    val isNameValid = remember(name) { name.length in 2..30 }
    val errorMessage by authViewModel.errorMessage.collectAsState()
    LaunchedEffect(Unit){
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
                // Allow only up to 30 characters
                if (newName.length <= 30) {
                    name = newName
                }
            },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        Spacer(Modifier.height(16.dp))

        // Continue Button
        Button(
            onClick = {
                if (isNameValid) {
                    authViewModel.verifyName(name)
                }
            },
            enabled = isNameValid // Button enabled only if name is valid
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
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun GenderScreen(authViewModel: AuthViewModel) {
    var gender by remember { mutableStateOf("") }
    LaunchedEffect(Unit){
        authViewModel.setErrorMessageToNull()
    }

    Column {
        // Radio Buttons for Gender Selection
        Row {
            StyledRadioButton(
                selected = gender == "Male",
                onClick = { gender = "Male" },
                label = "Male"
            )
            Spacer(Modifier.width(8.dp))
            StyledRadioButton(
                selected = gender == "Female",
                onClick = { gender = "Female" },
                label = "Female"
            )
            Spacer(Modifier.width(8.dp))
            StyledRadioButton(
                selected = gender == "Other",
                onClick = { gender = "Other" },
                label = "Other"
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (gender.isNotBlank()) {
                    authViewModel.verifyGender(gender)
                }
            },
            enabled = gender.isNotEmpty()
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
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf(0) }
    var selectedMonth by remember { mutableStateOf(0) }
    var selectedYear by remember { mutableStateOf(0) }
    val errorMessage by authViewModel.errorMessage.collectAsState()
    LaunchedEffect(Unit) {
        authViewModel.setErrorMessageToNull()
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.2f))
    ) {
        Text(
            text = selectedDate.ifEmpty { "Select Year of Birth" },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .clickable { showDatePicker = true }
                .padding(16.dp)

                .clip(RoundedCornerShape(12.dp))
                .fillMaxWidth()
        )
        if(errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.background,
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

        if (showDatePicker) {
            DatePickerDialogSample(
                onDismissRequest = { showDatePicker = false },
                onDateChange = { year, month, day ->
                    selectedYear = year
                    selectedMonth = month
                    selectedDay = day
                    selectedDate = formatDateToString(day, month, year)
                    showDatePicker = false
                }
            )
        }
    }
}

fun formatDateToString(day: Int, month: Int, year: Int): String {
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val daySuffix = when {
        day in 11..13 -> "th" // 11th, 12th, 13th
        day % 10 == 1 -> "st" // 1st, 21st, 31st
        day % 10 == 2 -> "nd" // 2nd, 22nd
        day % 10 == 3 -> "rd" // 3rd, 23rd
        else -> "th"          // 4th, 5th, etc.
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

    // Using a boolean state to control the display of the dialog
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
    Button(
        onClick = {
            // Trigger navigation to the app's main screen
            authViewModel.completeRegistration()
        }
    ) {
        Text("Continue")
    }
}