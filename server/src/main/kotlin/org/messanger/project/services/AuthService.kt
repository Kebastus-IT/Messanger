package org.messanger.project.services

import org.messanger.project.auth.PasswordHasher
import org.messanger.project.database.AuthRepository
import org.messanger.project.models.LoginRequest
import org.messanger.project.models.RegisterRequest
import java.util.*

sealed interface RegisterResult {
    data class Success(val token: String, val userId: String, val login: String, val displayName: String) : RegisterResult
    data object InvalidLogin: RegisterResult
    data object InvalidPassword: RegisterResult
    data object InvalidDisplayName: RegisterResult
    data object LoginTaken: RegisterResult
}
sealed interface LoginResult {
    data class Success(val token: String, val userId: String, val login: String, val displayName: String) : LoginResult
    data object InvalidCredentials: LoginResult
}
class AuthService(private val authRepo: AuthRepository, private val passwordHasher: PasswordHasher, private val jwtService: JwtService) {
    suspend fun register(req: RegisterRequest): RegisterResult{
        val login = req.login.trim()
        val displayName = req.displayName.trim()
        val password = req.password

        if (login.length !in 3..32 || !login.matches(Regex("[a-zA-Z0-9_]+$"))){
            return RegisterResult.InvalidLogin
        }
        if (displayName.length !in 1..64){
            return RegisterResult.InvalidDisplayName
        }
        if (password.length !in 8..128){
            return RegisterResult.InvalidPassword
        }
        if (authRepo.existsByLogin(login)) {
            return RegisterResult.LoginTaken
        }
        val passwordHash = passwordHasher.hash(password)

        val user = authRepo.createUser(
            id = UUID.randomUUID().toString(),
            login = login,
            displayName = displayName,
            passwordHash = passwordHash
        )

        val token = jwtService.createToken(user)

        return RegisterResult.Success(token,user.id, user.login, user.displayName)
    }
    suspend fun login(req: LoginRequest): LoginResult{
        val login = req.login.trim()
        if (req.login.isBlank() || req.password.isBlank()){
            return LoginResult.InvalidCredentials
        }
        val user = authRepo.findByLogin(login) ?: return LoginResult.InvalidCredentials

        val passwordOk = passwordHasher.verify(
            password = req.password,
            passwordHash = user.passwordHash
        )

        if (!passwordOk) {
            return LoginResult.InvalidCredentials
        }

        val token = jwtService.createToken(user)

        return LoginResult.Success(token,user.id, user.login, user.displayName)
    }
}