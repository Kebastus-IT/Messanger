package org.messanger.project.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.messanger.project.AuthApi
import org.messanger.project.UserSession

@Composable
fun AuthScreen(
    authApi: AuthApi,
    onAuthSuccess: (UserSession) -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }

    var login by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isLoginMode) "Login" else "Register",
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Login") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isLoginMode) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display name") },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        if (login.isBlank() || password.isBlank()) {
                            errorText = "Login and password must not be empty"
                            return@Button
                        }

                        if (!isLoginMode && displayName.isBlank()) {
                            errorText = "Display name must not be empty"
                            return@Button
                        }

                        scope.launch {
                            isLoading = true
                            errorText = null

                            try {
                                val response = if (isLoginMode) {
                                    authApi.login(
                                        login = login,
                                        password = password
                                    )
                                } else {
                                    authApi.register(
                                        login = login,
                                        displayName = displayName,
                                        password = password
                                    )
                                }

                                onAuthSuccess(
                                    UserSession(
                                        token = response.token,
                                        userId = response.userId,
                                        login = response.login,
                                        displayName = response.displayName
                                    )
                                )
                            } catch (e: Exception) {
                                errorText = e.message ?: "Authentication failed"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isLoading) {
                            "Please wait..."
                        } else {
                            if (isLoginMode) "Login" else "Create account"
                        }
                    )
                }

                TextButton(
                    onClick = {
                        isLoginMode = !isLoginMode
                        errorText = null
                    },
                    enabled = !isLoading,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        if (isLoginMode) {
                            "No account yet? Register"
                        } else {
                            "Already have an account? Login"
                        }
                    )
                }
            }
        }
    }
}