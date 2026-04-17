package org.messanger.project.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.messanger.project.connection.ChatConnection
import org.messanger.project.MessageBubble
import org.messanger.project.UiChatMessage
import org.messanger.project.UserSession
import org.messanger.project.models.ChatSummary
import org.messanger.project.protocol.*
@Composable
fun ChatScreen(
    session: UserSession,
    chat: ChatSummary,
    chatConnection: ChatConnection,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Connecting...") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val messages = remember { mutableStateListOf<UiChatMessage>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(chat.id) {
        chatConnection.connect(
            token = session.token,
            chatId = chat.id,
            scope = scope,
            onEvent = { event ->
                when (event) {
                    is Connected -> {
                        statusText = "Connected as ${event.userId}"
                    }
                    is JoinedChat -> {
                        statusText = "Joined chat"
                    }

                    is RecentMessages -> {
                        messages.clear()
                        messages.addAll(
                            event.items.map { item ->
                                UiChatMessage(
                                    serverMsgId = item.serverMsgId,
                                    senderUserId = item.senderUserId,
                                    senderDisplayName = item.senderDisplayName,
                                    text = item.text,
                                    isMine = item.senderUserId == session.userId,
                                    createdAt = item.createdAt
                                )
                            }
                        )
                    }

                    is ChatMessage -> {
                        messages.add(
                            UiChatMessage(
                                serverMsgId = event.serverMsgId,
                                senderUserId = event.senderUserId,
                                senderDisplayName = event.senderDisplayName,
                                text = event.text,
                                isMine = event.senderUserId == session.userId,
                                createdAt = event.createdAt
                            )
                        )
                    }

                    is ErrorEvent -> {
                        errorText = event.message
                    }
                }
            },
            onError = { message ->
                errorText = message
                statusText = "Connection failed"
            }
        )
    }

    DisposableEffect(chat.id) {
        onDispose {
            scope.launch {
                chatConnection.disconnect()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = chat.displayTitle,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            TextButton(onClick = onBack) {
                Text("Back")
            }
        }

        if (errorText != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorText!!,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Message") },
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    val trimmed = messageText.trim()
                    if (trimmed.isNotEmpty()) {
                        scope.launch {
                            try {
                                chatConnection.sendMessage(
                                    chatId = chat.id,
                                    text = trimmed
                                )
                                messageText = ""
                            } catch (e: Exception) {
                                errorText = e.message ?: "Failed to send message"
                            }
                        }
                    }
                }
            ) {
                Text("Send")
            }
        }
    }
}