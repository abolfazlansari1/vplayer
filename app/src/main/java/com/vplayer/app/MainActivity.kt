package com.vplayer.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            VPlayerTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppRoot(this@MainActivity, resumeTick)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick += 1
    }

    fun play(list: List<VideoItem>, index: Int) {
        Playlist.items = list.map { it.toPlayerItem() }
        Playlist.startIndex = index
        val i = Intent(this, PlayerActivity::class.java)
        i.putExtra(PlayerActivity.EXTRA_INDEX, index)
        startActivity(i)
    }

    fun playSingle(uri: Uri) {
        val item = PlayerItem(uri, displayName(this, uri), null)
        Playlist.items = listOf(item)
        Playlist.startIndex = 0
        val i = Intent(this, PlayerActivity::class.java)
        i.putExtra(PlayerActivity.EXTRA_INDEX, 0)
        startActivity(i)
    }
}

private fun videoPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private fun hasAllFilesAccess(): Boolean =
    if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager() else true

@Composable
private fun AppRoot(activity: MainActivity, resumeTick: Int) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, videoPermission()) == PackageManager.PERMISSION_GRANTED)
    }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) activity.playSingle(uri)
    }

    LaunchedEffect(Unit) {
        if (!granted) permLauncher.launch(videoPermission())
    }
    LaunchedEffect(granted, reload) {
        if (granted) {
            loading = true
            videos = withContext(Dispatchers.IO) { VideoRepo.load(ctx) }
            loading = false
        }
    }

    if (!granted) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Bg)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("برای نمایش ویدیوها، دسترسی به فایل‌های ویدیویی لازم است.", color = TextPrimary, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { permLauncher.launch(videoPermission()) }) { Text("اعطای دسترسی") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { filePicker.launch(arrayOf("video/*")) }) { Text("باز کردن یک فایل") }
        }
    } else {
        LibraryScreen(
            videos = videos,
            loading = loading,
            prefs = prefs,
            resumeTick = resumeTick,
            onPlay = { list, index -> activity.play(list, index) },
            onOpenFile = { filePicker.launch(arrayOf("video/*")) },
            onRefresh = { reload += 1 },
            onSettings = { showSettings = true },
        )
    }

    if (showSettings) {
        SettingsDialog(ctx, prefs) { showSettings = false }
    }
}

@Composable
private fun SettingsDialog(ctx: Context, prefs: Prefs, onClose: () -> Unit) {
    var autoSub by remember { mutableStateOf(prefs.autoSub) }
    val allFiles = hasAllFilesAccess()
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("بستن") } },
        title = { Text("تنظیمات") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("بارگذاری خودکار زیرنویس کنار ویدیو", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Switch(
                        checked = autoSub,
                        onCheckedChange = {
                            autoSub = it
                            prefs.autoSub = it
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Accent),
                    )
                }
                Text(
                    if (allFiles) "دسترسی به همه فایل‌ها: فعال"
                    else "برای پیدا کردن خودکار فایل‌های زیرنویس (srt, ass, vtt) کنار ویدیو، دسترسی به همه فایل‌ها لازم است. بدون آن می‌توانید زیرنویس را دستی انتخاب کنید.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
                if (!allFiles && Build.VERSION.SDK_INT >= 30) {
                    Button(onClick = {
                        val i = Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:" + ctx.packageName),
                        )
                        ctx.startActivity(i)
                    }) { Text("اعطای دسترسی به همه فایل‌ها") }
                }
            }
        },
    )
}
