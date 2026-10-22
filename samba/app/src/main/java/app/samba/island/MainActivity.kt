package app.samba.island

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private var refresh by mutableIntStateOf(0)
    private var pendingEnable = false

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        SambaStore.load(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = SambaColors.Neon,
                    onPrimary = SambaColors.Ink,
                    secondary = SambaColors.Violet,
                    tertiary = SambaColors.Mint,
                    background = SambaColors.Ink,
                    surface = SambaColors.Ink,
                    onBackground = Color.White,
                    onSurface = Color.White,
                ),
            ) {
                SambaScreen(
                    refresh = refresh,
                    onToggle = { on -> toggleIsland(on) },
                    onOverlaySettings = { openOverlaySettings() },
                    onAppSettings = { openAppSettings() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh++
        val canDraw = Settings.canDrawOverlays(this)

        // Pengguna baru kembali dari layar izin setelah menekan toggle.
        if (pendingEnable) {
            pendingEnable = false
            if (canDraw) {
                toggleIsland(true)
                return
            }
        }
        // Jaga island tetap sinkron dengan toggle.
        if (SambaStore.config.value.enabled) {
            if (canDraw) {
                ContextCompat.startForegroundService(this, Intent(this, IslandService::class.java))
            } else {
                SambaStore.update(this) { it.copy(enabled = false) }
            }
        }
    }

    private fun toggleIsland(on: Boolean) {
        val service = Intent(this, IslandService::class.java)
        if (!on) {
            SambaStore.update(this) { it.copy(enabled = false) }
            stopService(service)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            pendingEnable = true
            openOverlaySettings()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        SambaStore.update(this) { it.copy(enabled = true) }
        ContextCompat.startForegroundService(this, service)
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        )
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        )
    }
}

@Composable
private fun SambaScreen(
    refresh: Int,
    onToggle: (Boolean) -> Unit,
    onOverlaySettings: () -> Unit,
    onAppSettings: () -> Unit,
) {
    val context = LocalContext.current
    val cfg by SambaStore.config.collectAsState()
    val canOverlay = remember(refresh) { Settings.canDrawOverlays(context) }
    var previewExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SambaColors.Ink)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column {
            Text(
                "SAMBA",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
            )
            Text("Dynamic Island for Android", color = SambaColors.Muted, fontSize = 14.sp)
        }

        // Pratinjau: elemen utama layar ini.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1C3FB0), Color(0xFF0A0F2E)))),
            contentAlignment = Alignment.TopCenter,
        ) {
            IslandPill(
                compactWidth = cfg.width.dp,
                compactHeight = cfg.height.dp,
                expanded = previewExpanded,
                modifier = Modifier.padding(top = 14.dp),
                onClick = { previewExpanded = !previewExpanded },
            )
            Text(
                "Ketuk island untuk melihat animasinya",
                color = Color(0xB3FFFFFF),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Island", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (cfg.enabled) "Aktif di atas layar" else "Nonaktif",
                    color = SambaColors.Muted,
                    fontSize = 13.sp,
                )
            }
            Switch(
                checked = cfg.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SambaColors.Ink,
                    checkedTrackColor = SambaColors.Neon,
                    uncheckedThumbColor = SambaColors.Muted,
                    uncheckedTrackColor = Color(0xFF1A1D28),
                ),
            )
        }

        if (!canOverlay) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SambaColors.Violet, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Text("Izin belum aktif", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tanpa izin 'Tampil di atas aplikasi lain', island tidak bisa muncul di layar.",
                    color = SambaColors.Muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onOverlaySettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SambaColors.Neon,
                        contentColor = SambaColors.Ink,
                    ),
                ) {
                    Text("Aktifkan izin")
                }
            }
        }

        HorizontalDivider(color = SambaColors.Line)

        Column {
            Text("Posisi dan ukuran", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            SliderRow("Posisi vertikal", cfg.offsetY, 0..80) { v ->
                SambaStore.update(context) { it.copy(offsetY = v) }
            }
            SliderRow("Posisi horizontal", cfg.offsetX, -150..150) { v ->
                SambaStore.update(context) { it.copy(offsetX = v) }
            }
            SliderRow("Lebar", cfg.width, 80..220) { v ->
                SambaStore.update(context) { it.copy(width = v) }
            }
            SliderRow("Tinggi", cfg.height, 24..52) { v ->
                SambaStore.update(context) { it.copy(height = v) }
            }
            TextButton(
                onClick = { SambaStore.update(context) { cur -> IslandConfig(enabled = cur.enabled) } },
            ) {
                Text("Kembalikan ke bawaan", color = SambaColors.Neon)
            }
        }

        HorizontalDivider(color = SambaColors.Line)

        Column {
            Text("Cara mengaktifkan", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "1. Nyalakan toggle Island.\n" +
                    "2. Izinkan 'Tampil di atas aplikasi lain', lalu kembali ke SAMBA.\n" +
                    "3. Geser slider posisi sampai island pas dengan punch-hole kamera.\n\n" +
                    "Untuk menonaktifkan, matikan toggle atau cabut izin di pengaturan aplikasi.",
                color = SambaColors.Muted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
            TextButton(onClick = onAppSettings) {
                Text("Buka pengaturan aplikasi", color = SambaColors.Neon)
            }
        }

        Text("Versi 0.1.0 (MVP)", color = SambaColors.Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SliderRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column(Modifier.padding(top = 10.dp)) {
        Row {
            Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("$value dp", color = SambaColors.Muted, fontSize = 13.sp)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = SambaColors.Neon,
                activeTrackColor = SambaColors.Neon,
                inactiveTrackColor = Color(0xFF262A38),
            ),
        )
    }
}
