package app.samba.island

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service yang memasang island sebagai overlay (TYPE_APPLICATION_OVERLAY).
 * Service bukan Activity, jadi ia menjadi pemilik lifecycle/saved-state/viewmodel
 * untuk ComposeView secara manual.
 */
class IslandService : Service(), SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore
        get() = store

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var overlay: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        SambaStore.load(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Wajib memanggil startForeground cepat setelah startForegroundService.
        if (!startAsForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        val stopRequested = intent?.action == ACTION_STOP
        if (stopRequested || !Settings.canDrawOverlays(this)) {
            SambaStore.update(this) { it.copy(enabled = false) }
            stopSelf()
            return START_NOT_STICKY
        }
        if (overlay == null) showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        overlay?.let { runCatching { windowManager.removeView(it) } }
        overlay = null
        overlayParams = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        scope.cancel()
        super.onDestroy()
    }

    private fun showOverlay() {
        val density = resources.displayMetrics.density
        val cfg = SambaStore.config.value

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.x = (cfg.offsetX * density).toInt()
        params.y = (cfg.offsetY * density).toInt()
        // Izinkan island digambar di area punch-hole / notch.
        if (Build.VERSION.SDK_INT >= 30) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= 28) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@IslandService)
            setViewTreeSavedStateRegistryOwner(this@IslandService)
            setViewTreeViewModelStoreOwner(this@IslandService)
            setContent { IslandHost() }
        }

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            stopSelf()
            return
        }
        overlay = view
        overlayParams = params

        // Posisi mengikuti slider secara langsung.
        scope.launch {
            SambaStore.config.collect { c ->
                val lp = overlayParams ?: return@collect
                lp.x = (c.offsetX * density).toInt()
                lp.y = (c.offsetY * density).toInt()
                overlay?.let { runCatching { windowManager.updateViewLayout(it, lp) } }
            }
        }
    }

    private fun startAsForeground(): Boolean {
        return try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Island aktif", NotificationManager.IMPORTANCE_LOW)
            )
            val openApp = PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            val stop = PendingIntent.getService(
                this, 1,
                Intent(this, IslandService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE
            )
            val notification = Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_samba)
                .setContentTitle("SAMBA aktif")
                .setContentText("Island tampil di atas layar")
                .setContentIntent(openApp)
                .addAction(0, "Matikan", stop)
                .setOngoing(true)
                .build()
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIF_ID, notification)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        const val ACTION_STOP = "app.samba.island.STOP"
        private const val CHANNEL_ID = "samba_island"
        private const val NOTIF_ID = 1
    }
}
