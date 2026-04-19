package org.messanger.project.database


import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object UsersTable : Table("users") {
    val id = varchar("id", 64)
    val login = varchar("login", 64).uniqueIndex()
    val displayName = varchar("display_name", 128)
    val passwordHash = varchar("password_hash", 255)

    override val primaryKey = PrimaryKey(id)
}

data class AuthUser(
    val id: String,
    val login: String,
    val displayName: String,
    val passwordHash: String
)

private fun ResultRow.toAuthUser(): AuthUser {
    return AuthUser(
        id = this[UsersTable.id],
        login = this[UsersTable.login],
        displayName = this[UsersTable.displayName],
        passwordHash = this[UsersTable.passwordHash]
    )
}

class AuthRepository {

    suspend fun findByLogin(login: String): AuthUser? {
        return newSuspendedTransaction(Dispatchers.IO) {
            UsersTable
                .selectAll()
                .where { UsersTable.login eq login }
                .singleOrNull()
                ?.toAuthUser()
        }
    }

    suspend fun createUserIfLoginFree(
        id: String,
        login: String,
        displayName: String,
        passwordHash: String
    ): AuthUser? {
        return try {
            newSuspendedTransaction(Dispatchers.IO) {
                UsersTable.insert {
                    it[UsersTable.id] = id
                    it[UsersTable.login] = login
                    it[UsersTable.displayName] = displayName
                    it[UsersTable.passwordHash] = passwordHash
                }
                AuthUser(
                    id = id,
                    login = login,
                    displayName = displayName,
                    passwordHash = passwordHash
                )
            }
        } catch (e: ExposedSQLException){
            if (e.sqlState == "23505") null else throw e
        }
    }
}