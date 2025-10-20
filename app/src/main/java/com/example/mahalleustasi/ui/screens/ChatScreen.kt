package com.example.mahalleustasi.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mahalleustasi.data.model.Message
import com.example.mahalleustasi.ui.viewmodel.ChatViewModel
import com.example.mahalleustasi.ui.viewmodel.UsersViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    navController: NavController,
    chatId: String,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    var input by remember { mutableStateOf("") }
    val uid = Firebase.auth.currentUser?.uid
    val usersVm: UsersViewModel = hiltViewModel()

    // chatId formatı: job_{jobId}_{uidA}_{uidB}
    val parts = remember(chatId) { chatId.split("_") }
    val otherUid = remember(chatId, uid) {
        if (parts.size >= 4 && uid != null) {
            val a = parts[parts.size - 2]
            val b = parts[parts.size - 1]
            if (uid == a) b else if (uid == b) a else b
        } else null
    }

    LaunchedEffect(chatId) { viewModel.start(chatId) }
    DisposableEffect(Unit) { onDispose { viewModel.stop() } }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    val ctx = androidx.compose.ui.platform.LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val resolver = ctx.contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                val fileName = "img_${System.currentTimeMillis()}.jpg"
                viewModel.sendImage(chatId, bytes, fileName)
            }
        }
    }

    LaunchedEffect(otherUid) {
        otherUid?.let { usersVm.ensureUsers(listOf(it)) }
    }

    val otherUser = otherUid?.let { usersVm.userMap.collectAsState().value[it] }

    Column(modifier = Modifier.fillMaxSize()) {
        // Üst başlık: karşı kullanıcının avatarı ve adı
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val photo = otherUser?.photoUrl
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = (otherUser?.name?.firstOrNull()?.uppercase() ?: "?"),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Text(
                text = otherUser?.name ?: "Sohbet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Henüz mesaj yok", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            } else {
                val items = remember(messages) { buildChatItems(messages) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(items) { item ->
                        when (item) {
                            is ChatItem.Header -> {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text(
                                            text = item.text,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            is ChatItem.Bubble -> {
                                val isMe = uid != null && uid == item.message.senderId
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomEnd = if (isMe) 2.dp else 16.dp,
                                            bottomStart = if (isMe) 16.dp else 2.dp
                                        ),
                                        color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            item.message.imageUrl?.let { url ->
                                                AsyncImage(
                                                    model = url,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.size(180.dp).fillMaxWidth()
                                                )
                                            }
                                            item.message.text?.let { txt ->
                                                if (txt.isNotBlank()) Text(txt, style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                val time = SimpleDateFormat("HH:mm", Locale("tr")).format(Date(item.message.createdAt))
                                                Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                if (isMe) {
                                                    val read = item.message.readBy.isNotEmpty()
                                                    Text(if (read) "✓✓" else "✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { imagePicker.launch("image/*") }) {
                Icon(Icons.Outlined.Image, contentDescription = null)
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mesaj yaz") }
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                val text = input.trim()
                if (text.isNotEmpty()) {
                    viewModel.sendText(chatId, text)
                    input = ""
                }
            }) {
                Icon(Icons.Filled.Send, contentDescription = null)
            }
        }
    }
}

private sealed class ChatItem {
    data class Header(val text: String) : ChatItem()
    data class Bubble(val message: Message) : ChatItem()
}

private fun buildChatItems(list: List<Message>): List<ChatItem> {
    if (list.isEmpty()) return emptyList()
    val sdfDay = SimpleDateFormat("dd MMM yyyy", Locale("tr"))
    val items = mutableListOf<ChatItem>()
    var lastDay: String? = null
    for (m in list) {
        val day = sdfDay.format(Date(m.createdAt))
        if (day != lastDay) {
            items.add(ChatItem.Header(day))
            lastDay = day
        }
        items.add(ChatItem.Bubble(m))
    }
    return items
}
