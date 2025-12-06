package com.example.mahalleustasi.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mahalleustasi.data.model.Message
import com.example.mahalleustasi.ui.theme.* // Theme ve Color dosyalarini import ettik
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
    val error by viewModel.error.collectAsState()
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

    // DÜZELTME BURADA:
    // Listeyi burada olusturuyoruz ki boyutunu bilelim.
    val chatItems = remember(messages) { buildChatItems(messages) }

    // DÜZELTME BURADA:
    // messages.size degil, chatItems.size degistiginde kaydiriyoruz.
    LaunchedEffect(chatItems.size) {
        if (chatItems.isNotEmpty()) {
            listState.scrollToItem(chatItems.size - 1)
        }
    }

    LaunchedEffect(otherUid) {
        otherUid?.let { usersVm.ensureUsers(listOf(it)) }
    }

    val otherUser = otherUid?.let { usersVm.userMap.collectAsState().value[it] }

    Scaffold(
        containerColor = Color(0xFFE5DDD5), // WhatsApp tarzi arka plan veya MaterialTheme.colorScheme.background
        topBar = {
            ChatTopBar(
                navController = navController,
                otherUserName = otherUser?.name ?: "Sohbet",
                otherUserPhoto = otherUser?.photoUrl
            )
        },
        bottomBar = {
            ChatInputBar(
                chatId = chatId,
                viewModel = viewModel
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
            ) {
                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyChatState()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(chatItems) { item ->
                            when (item) {
                                is ChatItem.Header -> DateHeader(text = item.text)
                                is ChatItem.Bubble -> MessageBubble(
                                    message = item.message,
                                    isMe = uid == item.message.senderId
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------------------------- BİLEŞENLER --------------------------- //

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    navController: NavController,
    otherUserName: String,
    otherUserPhoto: String?
) {
    Surface(
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.primary // Ana Renk (Teal)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (otherUserPhoto != null) {
                        AsyncImage(model = otherUserPhoto, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Text(
                        text = otherUserName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            IconButton(onClick = { /* Menü */ }) {
                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isMe: Boolean) {
    // Renkleri Temadan Aliyoruz
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (isMe) Color.White else MahalleTextPrimary
    val timeColor = if (isMe) Color.White.copy(alpha = 0.7f) else MahalleTextSecondary

    // Baloncuk sekli (Kuyruk efekti)
    val shape = if (isMe) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(vertical = 2.dp)
        ) {
            Column(modifier = Modifier.padding(top = 8.dp, start = 10.dp, end = 10.dp, bottom = 6.dp)) {
                // Görsel Varsa
                message.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(bottom = 8.dp)
                    )
                }

                // Metin Varsa
                if (!message.text.isNullOrBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }

                // Saat ve Okundu Bilgisi
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val time = SimpleDateFormat("HH:mm", Locale("tr")).format(Date(message.createdAt))
                    Text(text = time, style = MaterialTheme.typography.labelSmall, color = timeColor, fontSize = 10.sp)

                    if (isMe) {
                        Spacer(Modifier.width(4.dp))
                        val isRead = message.readBy.isNotEmpty()
                        Text(
                            text = if (isRead) "✓✓" else "✓",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isRead) Color(0xFFB2DFDB) else timeColor,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = CircleShape
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ChatInputBar(chatId: String, viewModel: ChatViewModel) {
    var input by remember { mutableStateOf("") }
    val ctx = LocalContext.current
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

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp, // Hafif golge
        modifier = Modifier.fillMaxWidth().imePadding() // Klavye acilinca yukari iter
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            // Ekleme Butonu
            IconButton(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ekle", tint = MahalleTextSecondary)
            }

            // Yazı Alanı
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(24.dp))
                    .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (input.isEmpty()) {
                    Text("Mesaj yaz...", color = MahalleTextSecondary, style = MaterialTheme.typography.bodyLarge)
                }
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    textStyle = TextStyle(
                        color = MahalleTextPrimary,
                        fontSize = 16.sp
                    ),
                    maxLines = 4,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(8.dp))

            // Gönder Butonu (FAB Tarzi)
            FloatingActionButton(
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        viewModel.sendText(chatId, text)
                        input = ""
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary, // Teal
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun EmptyChatState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Sohbete Başla",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MahalleTextPrimary
        )
        Text(
            "Detayları konuşmak için bir mesaj yaz.",
            style = MaterialTheme.typography.bodyMedium,
            color = MahalleTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// --- YARDIMCI SINIFLAR ---

private sealed class ChatItem {
    data class Header(val text: String) : ChatItem()
    data class Bubble(val message: Message) : ChatItem()
}

private fun buildChatItems(list: List<Message>): List<ChatItem> {
    if (list.isEmpty()) return emptyList()
    val sdfDay = SimpleDateFormat("dd MMMM", Locale("tr"))
    val items = mutableListOf<ChatItem>()
    var lastDay: String? = null
    val today = sdfDay.format(Date())

    // Mesajlar eskiden yeniye sirali (ViewModel tarafindan saglanir)
    for (m in list) {
        val dayRaw = sdfDay.format(Date(m.createdAt))
        val day = if (dayRaw == today) "Bugün" else dayRaw

        if (day != lastDay) {
            items.add(ChatItem.Header(day))
            lastDay = day
        }
        items.add(ChatItem.Bubble(m))
    }
    return items
}