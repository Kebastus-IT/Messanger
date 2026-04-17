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
import org.messanger.project.models.UserSummary
import java.time.LocalDateTime

object ChatMembersTable : Table("chat_members") {
    val chatId = varchar("chat_id", 164)
    val userId = varchar("user_id", 64)

    override val primaryKey = PrimaryKey(chatId, userId)
}

object MessagesTable : Table("messages") {
    val msgId = long("id").autoIncrement()
    val chatId = varchar("chat_id", 164)
    val senderUserId = varchar("sender_user_id", 64)
    val text = text("text")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(msgId)
}

object ChatsTable : Table("chats") {
    val id = varchar("id", 164)
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
private data class RawChat(
    val id: String,
    val displayTitle: String,
    val type: String
)
private fun getRawUserChats(userId: String): List<RawChat> {
    return transaction {
        ChatMembersTable.join(
            otherTable = ChatsTable,
            joinType = org.jetbrains.exposed.sql.JoinType.INNER,
            onColumn = ChatMembersTable.chatId,
            otherColumn = ChatsTable.id
        )
            .select(ChatsTable.id, ChatsTable.title, ChatsTable.type)
            .where { ChatMembersTable.userId eq userId }
            .map {
                RawChat(
                    id = it[ChatsTable.id],
                    displayTitle = it[ChatsTable.title],
                    type = it[ChatsTable.type]
                )
            }
    }
}
private fun getDmTitles(
    currentUserId: String,
    dmChatIds: List<String>
): Map<String, String> {
    if (dmChatIds.isEmpty()) return emptyMap()

    return transaction {
        ChatMembersTable.join(
            otherTable = UsersTable,
            joinType = org.jetbrains.exposed.sql.JoinType.INNER,
            onColumn = ChatMembersTable.userId,
            otherColumn = UsersTable.id
        )
            .select(ChatMembersTable.chatId, UsersTable.displayName)
            .where {
                (ChatMembersTable.chatId inList dmChatIds) and
                        (ChatMembersTable.userId neq currentUserId)
            }
            .associate {
                val chatId = it[ChatMembersTable.chatId]
                val displayName = it[UsersTable.displayName]
                chatId to displayName
            }
    }
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
        val rawChats = getRawUserChats(userId)

        val dmChatIds = rawChats
            .filter { it.type == "DM" }
            .map { it.id }

        val dmTitles = getDmTitles(
            currentUserId = userId,
            dmChatIds = dmChatIds
        )

        return rawChats.map { rawChat ->
            val displayTitle = if (rawChat.type == "DM") {
                dmTitles[rawChat.id] ?: rawChat.displayTitle
            } else {
                rawChat.displayTitle
            }

            ChatSummary(
                id = rawChat.id,
                displayTitle = displayTitle,
                type = rawChat.type
            )
        }
    }

    fun findUser(
        query: String,
        ownId: String,
        limit: Int = 20
    ): List<UserSummary>{
        val trimmed = query.trim()
        if(trimmed.isBlank()) return emptyList()
        return transaction {
            UsersTable
                .select(UsersTable.id, UsersTable.login, UsersTable.displayName)
                .where{
                    (UsersTable.login like "%$trimmed%") and
                            (UsersTable.id neq ownId)
                }
                .limit(limit)
                .map {
                    UserSummary(
                        id = it[UsersTable.id],
                        login = it[UsersTable.login],
                        displayName = it[UsersTable.displayName]

                    )
                }

        }
    }

    fun buildChatIdForDM(userA : String, userB: String): String{
        val sorted = listOf(userA,userB).sorted()
        return "dm:${sorted[0]}:${sorted[1]}"
    }

    fun chatExists(chatId: String): Boolean{
        return transaction {
            ChatsTable
                .select(ChatsTable.id)
                .where { ChatsTable.id eq chatId }
                .limit(1)
                .any()
        }
    }
    fun createDMChat(chatId: String, userA: String, userB: String){
        return transaction {
            ChatsTable.insert {
                it[ChatsTable.id] = chatId
                it[ChatsTable.title] = "DM"
                it[ChatsTable.type] ="DM"
            }
            ChatMembersTable.insert {
                it[ChatMembersTable.chatId] = chatId
                it[ChatMembersTable.userId] = userA
            }
            ChatMembersTable.insert {
                it[ChatMembersTable.chatId] = chatId
                it[ChatMembersTable.userId] = userB
            }
        }
    }
    fun getUserChatById(userId: String, chatId: String): ChatSummary? {
        return transaction {
            ChatsTable.join(
                otherTable = ChatMembersTable,
                joinType = org.jetbrains.exposed.sql.JoinType.INNER,
                onColumn = ChatsTable.id,
                otherColumn = ChatMembersTable.chatId
            )
                .select(ChatsTable.id, ChatsTable.title, ChatsTable.type)
                .where{ (ChatMembersTable.userId eq userId) and (ChatsTable.id eq chatId)}
                .map { ChatSummary(
                    id = it[ChatsTable.id],
                    displayTitle =  it[ChatsTable.title],
                    type = it[ChatsTable.type]
                ) }
                .singleOrNull()
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