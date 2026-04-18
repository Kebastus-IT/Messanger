package org.messanger.project.auth

import de.mkammerer.argon2.Argon2Factory

class PasswordHasher {

    private val argon2 = Argon2Factory.create(
        Argon2Factory.Argon2Types.ARGON2id
    )

    fun hash(password: String): String {
        val iterations = 2
        val memoryKiB = 19 * 1024
        val parallelism = 1

        return argon2.hash(
            iterations,
            memoryKiB,
            parallelism,
            password.toCharArray()
        )
    }

    fun verify(password: String, passwordHash: String): Boolean {
        return argon2.verify(passwordHash, password.toCharArray())
    }
}