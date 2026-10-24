package com.talkbridge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
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
import kotlinx.coroutines.launch

private val Ink = Color(0xFF211B3D)
private val Violet = Color(0xFF6957E8)
private val Indigo = Color(0xFF33245D)
private val Teal = Color(0xFF36CDB6)
private val Coral = Color(0xFFFF819B)
private val Paper = Color(0xFFF7F4FF)
private val languages = listOf("English", "Yoruba", "Igbo", "Hausa", "French", "Spanish", "Arabic", "Chinese", "Portuguese", "German", "Swahili", "Italian", "Japanese", "Korean", "Turkish", "Hindi", "Dutch", "Russian")

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
    var source by remember { mutableStateOf("English") }
    var target by remember { mutableStateOf("Yoruba") }
    var input by remember { mutableStateOf("") }
    var languageFor by remember { mutableStateOf("") }
    var languageSearch by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Chats") }
    var dark by remember { mutableStateOf(false) }
    var fontScale by remember { mutableFloatStateOf(1f) }
    var wallpaper by remember { mutableStateOf("Aurora") }
    var autoTranslate by remember { mutableStateOf(true) }
    var profileName by remember { mutableStateOf("Your profile") }
    var profileBio by remember { mutableStateOf("Here to connect across languages") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val messages = remember {
        mutableStateListOf(
            ChatLine("Good morning! How are you?", "Ẹ káàárọ̀! Báwo ni ẹ ṣe wà?", true),
            ChatLine("Mo wà dáadáa, ẹ ṣé. Báwo ni ẹ?", "I’m fine, thank you. How are you?", false)
        )
    }
    val bg = if (dark) Color(0xFF151321) else Paper
    val fg = if (dark) Color(0xFFF8F5FF) else Ink
    val card = if (dark) Color(0xFF262238) else Color.White

    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Violet, secondary = Teal) else lightColorScheme(primary = Violet, secondary = Teal)) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = if (dark) Color(0xFF201B31) else Color.White,
                    modifier = Modifier.widthIn(max = 330.dp).fillMaxHeight()
                ) {
                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).background(Brush.linearGradient(listOf(Violet, Coral)), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("TalkBridge", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = fg)
                            Text("Make every language feel close", fontSize = 11.sp, color = Teal)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    listOf("Chats", "Live conversation", "Documents", "Profile", "Settings").forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item, fontWeight = if (selectedTab == item) FontWeight.Bold else FontWeight.Normal) },
                            selected = selectedTab == item,
                            onClick = {
                                selectedTab = item
                                scope.launch { drawerState.close() }
                            },
                            icon = {
                                Icon(when (item) {
                                    "Chats" -> Icons.Default.ChatBubble
                                    "Live conversation" -> Icons.Default.Mic
                                    "Documents" -> Icons.Default.Description
                                    "Profile" -> Icons.Default.Person
                                    else -> Icons.Default.Settings
                                }, contentDescription = null)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Your words. Their language. One conversation.", color = if (dark) Color.LightGray else Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(20.dp))
                }
            }
        ) {
            Scaffold(
                containerColor = bg,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(when (selectedTab) { "Live conversation" -> "Live conversation"; "Profile" -> "Your profile"; "Documents" -> "Documents"; "Settings" -> "Settings"; else -> "TalkBridge" }, fontWeight = FontWeight.ExtraBold, color = fg)
                                Text(when (selectedTab) { "Live conversation" -> "Talk naturally, both ways"; "Chats" -> "Translate without barriers"; else -> "Your space, your way" }, fontSize = 12.sp, color = Violet)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.ChatBubble, contentDescription = "Open menu", tint = fg)
                            }
                        },
                        actions = {
                            IconButton(onClick = { selectedTab = "Settings" }) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = fg) }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = card) {
                        listOf("Chats", "Live conversation", "Profile").forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(when (tab) { "Chats" -> Icons.Default.ChatBubble; "Live conversation" -> Icons.Default.Mic; else -> Icons.Default.Person }, contentDescription = null) },
                                label = { Text(when (tab) { "Live conversation" -> "Live"; else -> tab }) }
                            )
                        }
                    }
                }
            ) { padding ->
                when (selectedTab) {
                    "Settings" -> SettingsPage(dark, { dark = it }, fontScale, { fontScale = it }, wallpaper, { wallpaper = it }, autoTranslate, { autoTranslate = it }, fg, card)
                    "Profile" -> ProfilePage(profileName, { profileName = it }, profileBio, { profileBio = it }, source, target, fg, card)
                    "Documents" -> DocumentsPage(fg, card)
                    "Live conversation" -> LiveConversationPage(source, target, { languageFor = it; languageSearch = "" }, fg, card)
                    else -> Column(Modifier.fillMaxSize().padding(padding)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).background(card, RoundedCornerShape(22.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LanguageButton(source, fg) { languageFor = "source"; languageSearch = "" }
                            IconButton(onClick = { val old = source; source = target; target = old }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap languages", tint = Violet)
                            }
                            LanguageButton(target, fg) { languageFor = "target"; languageSearch = "" }
                        }
                        Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(Teal, CircleShape))
                            Spacer(Modifier.width(7.dp))
                            Text("TWO-WAY TRANSLATED CHAT", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp, color = Violet)
                            Spacer(Modifier.weight(1f))
                            Text("Auto-translate ${if (autoTranslate) "ON" else "OFF"}", fontSize = 10.sp, color = if (autoTranslate) Teal else Color.Gray)
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            item {
                                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Indigo, Violet, Color(0xFF9B79F4))), RoundedCornerShape(24.dp)).padding(18.dp)) {
                                    Column {
                                        Text("A conversation, not a monologue.", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(5.dp))
                                        Text("Both people can speak or type in their own language. Messages are designed to show the original and its translation together.", color = Color.White.copy(alpha = .86f), fontSize = 12.sp, lineHeight = 18.sp)
                                        Spacer(Modifier.height(12.dp))
                                        Text("SAMPLE CONVERSATION", color = Color.White.copy(alpha = .75f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                                    }
                                }
                            }
                            items(messages) { message ->
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (message.outgoing) Alignment.End else Alignment.Start) {
                                    Text(if (message.outgoing) "YOU · ${source.uppercase()}" else "OTHER PERSON · ${target.uppercase()}", fontSize = 9.sp, color = Violet, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                                    Column(
                                        Modifier.widthIn(max = 320.dp).background(
                                            if (message.outgoing) Brush.linearGradient(listOf(Violet, Color(0xFF9873F3)))
                                            else Brush.linearGradient(listOf(card, if (dark) card else Color(0xFFF0EBFF))),
                                            RoundedCornerShape(topStart = 21.dp, topEnd = 21.dp, bottomStart = if (message.outgoing) 21.dp else 6.dp, bottomEnd = if (message.outgoing) 6.dp else 21.dp)
                                        ).padding(15.dp)
                                    ) {
                                        Text(message.original, color = if (message.outgoing) Color.White else fg, fontSize = (15 * fontScale).sp, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.height(9.dp))
                                        HorizontalDivider(color = if (message.outgoing) Color.White.copy(alpha = .28f) else Violet.copy(alpha = .2f))
                                        Spacer(Modifier.height(8.dp))
                                        Text(message.translated, color = if (message.outgoing) Color.White.copy(alpha = .9f) else fg.copy(alpha = .8f), fontSize = (13 * fontScale).sp)
                                    }
                                    Text("Sample translation · not connected to live service", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, horizontal = 6.dp))
                                }
                            }
                        }
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).background(card, RoundedCornerShape(24.dp)).padding(8.dp)) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                placeholder = { Text("Type in ${source}…", fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                maxLines = 3
                            )
                            if (input.isNotBlank() && autoTranslate) {
                                Text("Live translation preview will appear here when a translation service is connected.", color = Violet, fontSize = 10.sp, modifier = Modifier.padding(start = 12.dp, top = 5.dp))
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { selectedTab = "Live conversation" }) { Icon(Icons.Default.Mic, contentDescription = "Open live conversation", tint = Violet) }
                                Text("Talk", color = Violet, fontSize = 11.sp)
                                Spacer(Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        val text = input.trim()
                                        if (text.isNotEmpty()) {
                                            messages.add(ChatLine(text, "Translation service not connected yet.", true))
                                            input = ""
                                        }
                                    },
                                    enabled = input.isNotBlank(),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Send")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (languageFor.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { languageFor = "" },
            title = { Text("Choose language") },
            text = {
                Column(Modifier.heightIn(max = 420.dp)) {
                    OutlinedTextField(value = languageSearch, onValueChange = { languageSearch = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search languages") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn {
                        items(languages.filter { it.contains(languageSearch, ignoreCase = true) }) { language ->
                            TextButton(onClick = {
                                if (languageFor == "source") source = language else target = language
                                languageFor = ""
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(language, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { languageFor = "" }) { Text("Close") } }
        )
    }
}

@Composable
private fun LanguageButton(language: String, fg: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(language, fontWeight = FontWeight.Bold, color = fg)
            Text("Choose language", fontSize = 9.sp, color = Violet)
        }
    }
}

@Composable
private fun SettingsPage(dark: Boolean, onDark: (Boolean) -> Unit, scale: Float, onScale: (Float) -> Unit, wallpaper: String, onWallpaper: (String) -> Unit, auto: Boolean, onAuto: (Boolean) -> Unit, fg: Color, card: Color) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Make TalkBridge yours", color = fg, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text("Appearance, reading comfort and translation preferences.", color = Color.Gray, fontSize = 13.sp)
        PreferenceCard(card) {
            PreferenceSwitch("Dark appearance", "A softer screen for low light", dark, onDark, fg)
            HorizontalDivider(color = Violet.copy(alpha = .12f))
            PreferenceSwitch("Auto-translate while typing", "Preview area appears as you type", auto, onAuto, fg)
        }
        PreferenceCard(card) {
            Text("Chat text size", color = fg, fontWeight = FontWeight.SemiBold)
            Slider(value = scale, onValueChange = onScale, valueRange = 0.85f..1.35f)
            Text("Wallpaper style", color = fg, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Aurora", "Lavender", "Ocean").forEach { choice ->
                    FilterChip(selected = wallpaper == choice, onClick = { onWallpaper(choice) }, label = { Text(choice) })
                }
            }
            Text("Photo wallpapers and custom images are planned; they are not enabled in this build.", fontSize = 11.sp, color = Color.Gray)
        }
        Text("Live voice, translated audio, real-time text translation and cloud messaging still need secure service connections.", color = Color.Gray, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun PreferenceCard(card: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(card, RoundedCornerShape(22.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun PreferenceSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit, fg: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = fg, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LiveConversationPage(source: String, target: String, chooseLanguage: (String) -> Unit, fg: Color, card: Color) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Speak to each other naturally.", fontSize = 24.sp, color = fg, fontWeight = FontWeight.ExtraBold)
        Text("A two-person conversation mode: each person gets their own language, transcript and translated reply.", color = Color.Gray, fontSize = 13.sp, lineHeight = 19.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            LanguageButton(source, fg) { chooseLanguage("source") }
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Violet)
            LanguageButton(target, fg) { chooseLanguage("target") }
        }
        ConversationSide("PERSON A · $source", "You speak $source", Violet, card, fg)
        ConversationSide("PERSON B · $target", "They speak $target", Teal, card, fg)
        Spacer(Modifier.weight(1f))
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Live microphone translation — coming next")
        }
        Text("This screen is the interface preview. Microphone capture, speech recognition and translated audio are not connected yet.", color = Color.Gray, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun ConversationSide(title: String, subtitle: String, accent: Color, card: Color, fg: Color) {
    Row(Modifier.fillMaxWidth().background(card, RoundedCornerShape(22.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(accent.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(subtitle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg)
            Text("Transcript and translation appear here", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun DocumentsPage(fg: Color, card: Color) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Translate documents", color = fg, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("A future home for PDFs, Word files and text documents.", color = Color.Gray, fontSize = 13.sp)
        Column(Modifier.fillMaxWidth().background(card, RoundedCornerShape(24.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Description, contentDescription = null, tint = Violet, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(12.dp))
            Text("Document translation is coming next", color = fg, fontWeight = FontWeight.Bold)
            Text("File import, translated downloads and formatting preservation are not connected in this version.", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProfilePage(name: String, onName: (String) -> Unit, bio: String, onBio: (String) -> Unit, source: String, target: String, fg: Color, card: Color) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Indigo, Violet, Coral)), RoundedCornerShape(26.dp)).padding(22.dp)) {
            Column {
                Box(Modifier.size(64.dp).background(Color.White.copy(alpha = .2f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(name, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text("Your TalkBridge identity", color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
            }
        }
        OutlinedTextField(value = name, onValueChange = onName, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true)
        OutlinedTextField(value = bio, onValueChange = onBio, label = { Text("About you") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), minLines = 2)
        Text("Languages: $source ↔ $target", color = fg, fontWeight = FontWeight.SemiBold)
        Text("Account sign-in, profile photo and cloud sync are not connected yet.", color = Color.Gray, fontSize = 12.sp)
    }
}
