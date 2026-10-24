package com.talkbridge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF17152F)
private val Violet = Color(0xFF7665FF)
private val Cyan = Color(0xFF49D8D0)
private val SurfaceLight = Color(0xFFF7F7FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TalkBridgeApp() }
    }
}

private data class ChatLine(val original: String, val translated: String, val outgoing: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TalkBridgeApp() {
    var sourceLanguage by remember { mutableStateOf("English") }
    var targetLanguage by remember { mutableStateOf("Yoruba") }
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(false) }
    var fontScale by remember { mutableFloatStateOf(1f) }
    var demoMode by remember { mutableStateOf(true) }
    val messages = remember {
        mutableStateListOf(
            ChatLine("Good morning! How are you?", "Ẹ káàárọ̀! Báwo ni ẹ ṣe wà?", true),
            ChatLine("Mo wà dáadáa, ẹ ṣé.", "I am fine, thank you.", false)
        )
    }
    val background = if (darkMode) Color(0xFF11111C) else SurfaceLight
    val foreground = if (darkMode) Color(0xFFF4F2FF) else Ink

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme(primary = Violet) else lightColorScheme(primary = Violet)) {
        Scaffold(
            containerColor = background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("TalkBridge", fontWeight = FontWeight.Bold, color = foreground)
                            Text("Translate without barriers", fontSize = 12.sp, color = if (darkMode) Cyan else Violet)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = foreground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = background)
                )
            }
        ) { padding ->
            if (showSettings) {
                Column(Modifier.fillMaxSize().padding(padding).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("Appearance & accessibility", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = foreground)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Dark theme", color = foreground)
                        Switch(checked = darkMode, onCheckedChange = { darkMode = it })
                    }
                    Text("Text size", color = foreground)
                    Slider(value = fontScale, onValueChange = { fontScale = it }, valueRange = 0.85f..1.35f)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Demo translations", color = foreground)
                        Switch(checked = demoMode, onCheckedChange = { demoMode = it })
                    }
                    Text("Live translation services and document import are not connected yet.", color = foreground.copy(alpha = .7f), fontSize = 13.sp)
                    Button(onClick = { showSettings = false }) { Text("Done") }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                            .background(if (darkMode) Color(0xFF242337) else Color.White, RoundedCornerShape(18.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LanguageButton(sourceLanguage) { sourceLanguage = nextLanguage(sourceLanguage) }
                        IconButton(onClick = { val old = sourceLanguage; sourceLanguage = targetLanguage; targetLanguage = old }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap languages", tint = Violet)
                        }
                        LanguageButton(targetLanguage) { targetLanguage = nextLanguage(targetLanguage) }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        item {
                            Text("CONVERSATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (darkMode) Cyan else Violet, letterSpacing = 1.5.sp)
                        }
                        items(messages) { message ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (message.outgoing) Alignment.End else Alignment.Start
                            ) {
                                Column(
                                    Modifier.widthIn(max = 310.dp)
                                        .background(
                                            if (message.outgoing) Brush.linearGradient(listOf(Violet, Color(0xFF9D65F8)))
                                            else Brush.linearGradient(listOf(if (darkMode) Color(0xFF29283B) else Color.White, if (darkMode) Color(0xFF29283B) else Color(0xFFF0EEFA))),
                                            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (message.outgoing) 20.dp else 5.dp, bottomEnd = if (message.outgoing) 5.dp else 20.dp)
                                        )
                                        .padding(15.dp)
                                ) {
                                    Text(message.original, color = if (message.outgoing) Color.White else foreground, fontSize = (15 * fontScale).sp)
                                    Spacer(Modifier.height(8.dp))
                                    HorizontalDivider(color = if (message.outgoing) Color.White.copy(alpha = .25f) else Violet.copy(alpha = .2f))
                                    Spacer(Modifier.height(7.dp))
                                    Text(message.translated, color = if (message.outgoing) Color.White.copy(alpha = .9f) else foreground.copy(alpha = .8f), fontSize = (13 * fontScale).sp)
                                }
                                Text(if (message.outgoing) "✓✓  Translated" else "Translated", fontSize = 10.sp, color = if (darkMode) Color.LightGray else Color.Gray, modifier = Modifier.padding(top = 4.dp, horizontal = 5.dp))
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp).background(if (darkMode) Color(0xFF242337) else Color.White, RoundedCornerShape(24.dp)).padding(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("Write something to translate…", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            maxLines = 4
                        )
                        Spacer(Modifier.width(7.dp))
                        Button(
                            onClick = {
                                val text = input.trim()
                                if (text.isNotEmpty()) {
                                    val translated = if (demoMode) "Demo mode — connect a translation provider to translate this message." else "Translation service is not configured yet."
                                    messages.add(ChatLine(text, translated, true))
                                    input = ""
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 13.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) { Text("Send") }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageButton(language: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(language, fontWeight = FontWeight.SemiBold)
            Text("Tap to change", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

private fun nextLanguage(current: String): String {
    val languages = listOf("English", "Yoruba", "Igbo", "Hausa", "French", "Spanish", "Arabic", "Chinese")
    return languages[(languages.indexOf(current).coerceAtLeast(0) + 1) % languages.size]
}
