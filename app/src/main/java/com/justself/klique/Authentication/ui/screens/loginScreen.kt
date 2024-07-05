package com.justself.klique.Authentication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.justself.klique.Authentication.ui.viewModels.AuthViewModel
import com.justself.klique.Authentication.ui.viewModels.LoginState
import com.justself.klique.MyAppTheme
import com.justself.klique.R


@Composable
fun LoginScreen(navController: NavHostController, authViewModel: AuthViewModel = viewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailHasFocus by remember { mutableStateOf(false) }
    var passwordHasFocus by remember { mutableStateOf(false) }

    // Observing login state and error messages
    val loginState by authViewModel.loginState.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    // Navigate to "home" upon successful login
    if (loginState == LoginState.SUCCESS) {
        LaunchedEffect(Unit) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    MyAppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.pink_lipstick),
                contentDescription = "Background Image",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "You are not logged in. Please log in.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.height(16.dp))

                CustomTextField(
                    text = email,
                    onTextChange = { email = it },
                    label = "Email",
                    hasFocus = emailHasFocus,
                    onFocusChange = { emailHasFocus = it },
                    isPassword = false
                )

                Spacer(Modifier.height(8.dp))

                CustomTextField(
                    text = password,
                    onTextChange = { password = it },
                    label = "Password",
                    hasFocus = passwordHasFocus,
                    onFocusChange = { passwordHasFocus = it },
                    isPassword = true
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { authViewModel.login(email, password) },
                    enabled = email.isNotEmpty() && password.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Login", style = MaterialTheme.typography.bodyLarge)
                }

                // Display error message when there is an error state
                if (loginState == LoginState.ERROR && errorMessage.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}


@Composable
fun CustomTextField(
    text: String,
    onTextChange: (String) -> Unit,
    label: String,
    hasFocus: Boolean,
    onFocusChange: (Boolean) -> Unit,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None

    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        visualTransformation = visualTransformation,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimary), // Existing text style
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary), // Custom cursor color
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChange(it.hasFocus) }
            .background(Color.Transparent, MaterialTheme.shapes.small),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(modifier = Modifier.weight(1f).padding(end = 32.dp)) {
                    if (!hasFocus && text.isEmpty()) {
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    innerTextField()  // Apply innerTextField without direct modifier
                }
                if (isPassword) {
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            id = if (passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                        ),
                        contentDescription = "Toggle Password Visibility",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)  // Provide padding for easier tapping
                            .clickable { passwordVisible = !passwordVisible },
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    )
}