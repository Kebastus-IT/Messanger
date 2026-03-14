package org.messanger.project.auth


import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val login: String,
    val displayName: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val login: String,
    val displayName: String
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String
)