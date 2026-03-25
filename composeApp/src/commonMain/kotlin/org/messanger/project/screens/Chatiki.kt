package org.messanger.project.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.messanger.project.connection.ChatsApi
import org.messanger.project.connection.CreateChatApi
import org.messanger.project.connection.FindUserApi
import org.messanger.project.models.ChatSummary
import org.messanger.project.models.UserSummary


@Composable
fun ChatsScreen(
    token: String,
    chatsApi: ChatsApi,
    findUserApi: FindUserApi,
    createChatApi: CreateChatApi,
    onChatSelected: (ChatSummary) -> Unit,
    onLogout: () -> Unit
) {
    var chats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var newChatOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var foundUsers by remember { mutableStateOf<List<UserSummary>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var creatingDM by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun loadChats() {
        scope.launch {
            isLoading = true
            errorText = null

            try {
                chats = chatsApi.getChats(token)
            } catch (e: Exception) {
                errorText = e.message ?: "Unknown error"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadChats()
    }
    LaunchedEffect(searchQuery, newChatOpen){
        if(!newChatOpen) return@LaunchedEffect
        val  trimmed = searchQuery.trim()
        if(trimmed.isEmpty()){
            foundUsers = emptyList()
            searchError = null
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        searchError = null

        delay(300)
        try {
            foundUsers = findUserApi.findUser(
                token = token,
                loginQuery = trimmed
            )
        } catch (e: Exception) {
            foundUsers = emptyList()
            searchError = e.message?: "Failed to search users"
        }finally {
            isSearching = false
        }
    }
    if(newChatOpen){
        Dialog(
            onDismissRequest = {
                if (!creatingDM){
                    newChatOpen = false
                    searchQuery = ""
                    foundUsers = emptyList()
                    searchError = null
                }
            }
        ){
            Card (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ){
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "New Chat",
                        style = MaterialTheme.typography.headlineSmall
                        )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {searchQuery = it},
                        label = { Text("Search by Login")},
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ){
                        Text("Create group chat")
                    }
                    when {
                        creatingDM -> {
                            Row (
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ){
                                CircularProgressIndicator()
                            }
                        }

                        isSearching -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        searchError != null -> {
                            Text(
                                text = searchError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        searchQuery.trim().isEmpty() -> {
                            Text(
                                text = "Start typing a login to search users",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        foundUsers.isEmpty() -> {
                            Text(
                                text = "No users found",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(foundUsers) { user ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !creatingDM) {
                                                scope.launch {
                                                    creatingDM = true
                                                    searchError = null

                                                    try {
                                                        val chat = createChatApi.getOrCreateChat(
                                                            token = token,
                                                            otherUserId = user.id
                                                        )

                                                        newChatOpen = false
                                                        searchQuery = ""
                                                        foundUsers = emptyList()
                                                        onChatSelected(chat)
                                                    } catch (e: Exception) {
                                                        searchError = e.message ?: "Failed to open chat"
                                                    } finally {
                                                        creatingDM = false
                                                    }
                                                }
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                text = user.displayName,
                                                style = MaterialTheme.typography.titleMedium
                                            )

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                text = "@${user.login}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            if (!creatingDM) {
                                newChatOpen = false
                                searchQuery = ""
                                foundUsers = emptyList()
                                searchError = null
                            }
                        },
                        enabled = !creatingDM,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
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
            Text(
                text = "Chats",
                style = MaterialTheme.typography.headlineMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        newChatOpen = true
                    }
                ) {
                    Text("New chat")
                }

                TextButton(onClick = onLogout) {
                    Text("Logout")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }

            errorText != null -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Failed to load chats: $errorText",
                        color = MaterialTheme.colorScheme.error
                    )

                    Button(onClick = { loadChats() }) {
                        Text("Retry")
                    }
                }
            }

            chats.isEmpty() -> {
                Text("No chats yet")
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chats) { chat ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChatSelected(chat) }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = chat.displayTitle,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}