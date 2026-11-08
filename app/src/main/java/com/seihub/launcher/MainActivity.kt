package com.seihub.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---- Edit these ----
const val DISCORD_URL = "https://discord.com"
const val YOUTUBE_URL = "https://youtube.com"
const val MC = "com.mojang.minecraftpe"

private val Bg = Color(0xFF0D0B14)
private val Panel = Color(0xFF14111F)
private val Purple = Color(0xFF6D3DF0)
private val Green = Color(0xFF5FD91A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Bg, surface = Panel, onSurface = Color.White)) { App() }
        }
    }
}

fun mcVersion(c: Context, p: String): String? =
    try { c.packageManager.getPackageInfo(p, 0).versionName } catch (e: Exception) { null }

fun openUrl(c: Context, u: String) {
    try { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {}
}

fun launchMc(c: Context, p: String) {
    val i = c.packageManager.getLaunchIntentForPackage(p)
    if (i != null) c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) else openUrl(c, "https://play.google.com/store/apps/details?id=$p")
}

fun importPack(c: Context, uri: Uri, p: String) {
    val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/octet-stream").setPackage(p)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    try { c.startActivity(i) } catch (e: Exception) {
        Toast.makeText(c, "Minecraft can't open this file", Toast.LENGTH_LONG).show()
    }
}

fun fileName(c: Context, u: Uri): String =
    c.contentResolver.query(u, null, null, null, null)?.use {
        val ix = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && ix >= 0) it.getString(ix) else null
    } ?: "addon"

@Composable
fun App() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("sei", Context.MODE_PRIVATE) }
    var tab by remember { mutableIntStateOf(0) }
    var pkg by remember { mutableStateOf(prefs.getString("pkg", MC) ?: MC) }
    var name by remember { mutableStateOf(prefs.getString("name", "SEI Player") ?: "SEI Player") }
    var recent by remember { mutableStateOf((prefs.getString("recent", "") ?: "").lines().filter { it.isNotBlank() }) }
    fun saveRecent(l: List<String>) { recent = l; prefs.edit().putString("recent", l.joinToString("\n")).apply() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
            val entry = "$uri\t${fileName(ctx, uri)}"
            saveRecent((listOf(entry) + recent.filter { it != entry }).take(30))
            importPack(ctx, uri, pkg)
        }
    }
    val tabs = listOf("Play" to Icons.Filled.PlayArrow, "Addons" to Icons.Filled.Add, "Settings" to Icons.Filled.Settings)

    Scaffold(containerColor = Bg, bottomBar = {
        NavigationBar(containerColor = Panel) {
            tabs.forEachIndexed { i, (label, icon) ->
                NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(icon, label) }, label = { Text(label) })
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> PlayScreen(name, pkg) { tab = 2 }
                1 -> AddonScreen(recent,
                    onPick = { picker.launch(arrayOf("*/*")) },
                    onAgain = { importPack(ctx, Uri.parse(it), pkg) },
                    onRemove = { saveRecent(recent - it) })
                else -> SettingsScreen(name, pkg,
                    onName = { name = it; prefs.edit().putString("name", it).apply() },
                    onPkg = { pkg = it; prefs.edit().putString("pkg", it).apply() })
            }
        }
    }
}

@Composable
fun PlayScreen(name: String, pkg: String, onManage: () -> Unit) {
    val ctx = LocalContext.current
    val ver = remember(pkg) { mcVersion(ctx, pkg) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("SEI Launcher v1", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0C1512), Color(0xFF1F6B57)))).padding(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Minecraft Bedrock", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text(if (ver != null) "Installed · v$ver" else "Not installed", color = if (ver != null) Green else Color(0xFFFF5A6E))
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { launchMc(ctx, pkg) },
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color(0xFF10230A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text(if (ver != null) "LAUNCH" else "GET MINECRAFT", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.Person, null, tint = Purple)
                Text(name, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = onManage) { Text("Manage") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { openUrl(ctx, DISCORD_URL) }, modifier = Modifier.weight(1f)) { Text("Discord") }
            OutlinedButton(onClick = { openUrl(ctx, YOUTUBE_URL) }, modifier = Modifier.weight(1f)) { Text("YouTube") }
        }
    }
}

@Composable
fun AddonScreen(recent: List<String>, onPick: () -> Unit, onAgain: (String) -> Unit, onRemove: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Addons", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Button(onClick = onPick, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Import .mcpack / .mcaddon / .mcworld") }
        if (recent.isEmpty()) Text("No files yet. Pick a file and it opens in Minecraft for import.", color = Color.Gray)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recent) { e ->
                val parts = e.split("\t")
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(parts.getOrElse(1) { "addon" }, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1)
                        TextButton(onClick = { onAgain(parts[0]) }) { Text("Import") }
                        IconButton(onClick = { onRemove(e) }) { Icon(Icons.Filled.Delete, "Remove", tint = Color.Gray) }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(name: String, pkg: String, onName: (String) -> Unit, onPkg: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = name, onValueChange = onName, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pkg, onValueChange = onPkg, label = { Text("Minecraft package") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Default: $MC", color = Color.Gray, fontSize = 12.sp)
    }
}

