package org.messanger.project.database

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object ChatMembersTable : Table("chat_members") {
    val chatId = varchar("chat_id", 64)
    val userId = varchar("user_id", 64)

    override val primaryKey = PrimaryKey(chatId, userId)
}

object MessagesTable : Table("messages") {
    val id = long("id").autoIncrement()
    val chatId = varchar("chat_id", 64)
    val senderUserId = varchar("sender_user_id", 64)
    val text = text("text")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

data class StoredMessage(
    val id: Long,
    val chatId: String,
    val senderUserId: String,
    val text: String
)

private fun ResultRow.toStoredMessage(): StoredMessage {
    return StoredMessage(
        id = this[MessagesTable.id],
        chatId = this[MessagesTable.chatId],
        senderUserId = this[MessagesTable.senderUserId],
        text = this[MessagesTable.text]
    )
}
object ChatRepository {
    fun isMember(chatId: String, userId: String): Boolean {
        return transaction {
            ChatMembersTable
                .selectAll()
                .where {
                    (ChatMembersTable.chatId eq chatId) and
                            (ChatMembersTable.userId eq userId)
                }
                .count() > 0
        }
    }

    fun saveMessage(chatId: String, userId: String, text: String): Long {
        return transaction {
            val inserted = MessagesTable.insert {
                it[MessagesTable.chatId] = chatId
                it[MessagesTable.senderUserId] = userId
                it[MessagesTable.text] = text
            }

            inserted[MessagesTable.id]
        }
    }

    fun getChatMemberIds(chatId: String): List<String> {
        return transaction {
            ChatMembersTable
                .select(ChatMembersTable.userId)
                .where { ChatMembersTable.chatId eq chatId }
                .map { it[ChatMembersTable.userId] }
        }
    }
    fun getRecentMessages(chatId: String, limit: Int): List<StoredMessage> {
        return transaction {
            MessagesTable
                .selectAll()
                .where { MessagesTable.chatId eq chatId }
                .orderBy(MessagesTable.id, SortOrder.DESC)
                .limit(limit)
                .map { it.toStoredMessage() }
                .reversed()
        }
    }
}