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
import org.messanger.project.models.ChatSummary
import java.time.LocalDateTime

object ChatMembersTable : Table("chat_members") {
    val chatId = varchar("chat_id", 64)
    val userId = varchar("user_id", 64)

    override val primaryKey = PrimaryKey(chatId, userId)
}

object MessagesTable : Table("messages") {
    val msgId = long("id").autoIncrement()
    val chatId = varchar("chat_id", 64)
    val senderUserId = varchar("sender_user_id", 64)
    val text = text("text")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(msgId)
}

object ChatsTable : Table("chats") {
    val id = varchar("id", 64)
    val title = varchar("title", 255)
    val type = varchar("type", 32)

    override val primaryKey = PrimaryKey(id)
}

data class StoredMessage(
    val id: Long,
    val chatId: String,
    val senderUserId: String,
    val senderDisplayName: String,
    val text: String,
    val createdAt: LocalDateTime
)

private fun ResultRow.toStoredMessage(): StoredMessage {
    return StoredMessage(
        id = this[MessagesTable.msgId],
        chatId = this[MessagesTable.chatId],
        senderUserId = this[MessagesTable.senderUserId],
        senderDisplayName = this[MessagesTable.text],
        text = this[MessagesTable.text],
        createdAt =  this[MessagesTable.createdAt]
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

            inserted[MessagesTable.msgId]
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

    fun getUserChats(userId: String): List<ChatSummary> {
        return transaction {
            ChatMembersTable.join(
                otherTable = ChatsTable,
                joinType = org.jetbrains.exposed.sql.JoinType.INNER,
                onColumn = ChatMembersTable.chatId,
                otherColumn = ChatsTable.id
            )
                .select(ChatsTable.id, ChatsTable.title)
                .where { ChatMembersTable.userId eq userId }
                .map {
                    ChatSummary(
                        id = it[ChatsTable.id],
                        title = it[ChatsTable.title]
                    )
                }
        }
    }
    fun getUserDisplayName(userId: String): String? {
        return transaction {
            UsersTable
                .select(UsersTable.displayName)
                .where { UsersTable.id eq userId }
                .map { it[UsersTable.displayName] }
                .singleOrNull()
        }
    }
    fun getRecentMessages(chatId: String, limit: Int): List<StoredMessage> {
        return transaction {
            MessagesTable.join(
                otherTable = UsersTable,
                joinType = org.jetbrains.exposed.sql.JoinType.INNER,
                onColumn = MessagesTable.senderUserId,
                otherColumn = UsersTable.id
            )
                .selectAll()
                .where { MessagesTable.chatId eq chatId }
                .orderBy(MessagesTable.msgId, SortOrder.DESC)
                .limit(limit)
                .map {
                    StoredMessage(
                        id = it[MessagesTable.msgId],
                        chatId = it[MessagesTable.chatId],
                        senderUserId = it[MessagesTable.senderUserId],
                        senderDisplayName = it[UsersTable.displayName],
                        text = it[MessagesTable.text],
                        createdAt = it[MessagesTable.createdAt]
                    )
                }
                .reversed()
        }
    }
}