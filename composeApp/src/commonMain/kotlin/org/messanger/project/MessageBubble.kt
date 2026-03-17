package org.messanger.project

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageBubble(
    message: UiChatMessage
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (message.isMine) "You" else message.fromUserId,
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge
            )

            if (message.createdAt != null) {
                Text(
                    text = message.createdAt,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}