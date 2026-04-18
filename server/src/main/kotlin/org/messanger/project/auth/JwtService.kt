package org.messanger.project.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.messanger.project.database.AuthUser
import java.util.Date

class JwtService(
    private val secret: String,
    private val issuer: String,
    private val audience: String,
) {
    private val algorithm = Algorithm.HMAC256(secret)

    fun createToken(user: AuthUser): String {
        val now = System.currentTimeMillis()
        val expiresAt = Date(now + 1000L * 60 * 60 * 24 * 7)

        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", user.id)
            .withClaim("login", user.login)
            .withClaim("displayName", user.displayName)
            .withExpiresAt(expiresAt)
            .sign(algorithm)
    }
}