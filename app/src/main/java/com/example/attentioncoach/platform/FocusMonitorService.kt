package com.example.attentioncoach.platform

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.attentioncoach.domain.FocusMonitorCadence
import com.example.attentioncoach.domain.ForegroundPresenceClassifier
import com.example.attentioncoach.domain.ForegroundPresenceMemory
import com.example.attentioncoach.domain.FocusPresence
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.SoftLockPolicy

class FocusMonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usageStatsBoundary: UsageStatsBoundary
    private lateinit var foregroundObservationStore: ForegroundObservationStore
    private lateinit var launcherPackagesProvider: LauncherPackagesProvider
    private lateinit var notifier: ReentryNotifier
    private var session: MonitorSession? = null
    private var lastNotificationMillis: Long? = null
    private var lastStablePresence: FocusPresence? = null

    private val monitorTick = object : Runnable {
        override fun run() {
            scanForegroundApp()
            handler.postDelayed(this, FocusMonitorCadence.POLL_INTERVAL_MILLIS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        usageStatsBoundary = UsageStatsBoundary(this)
        foregroundObservationStore = ForegroundObservationStore(this)
        launcherPackagesProvider = LauncherPackagesProvider(this)
        notifier = ReentryNotifier(this)
        notifier.ensureChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring(intent)
            ACTION_STOP -> stopMonitoring()
            ACTION_RESET_REENTRY_COOLDOWN -> resetReentryCooldown(intent)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(monitorTick)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoring(intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, INVALID_TASK_ID)
        if (taskId == INVALID_TASK_ID) {
            stopMonitoring()
            return
        }

        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty()
        session = MonitorSession(
            taskId = taskId,
            taskTitle = taskTitle,
            neededPackages = intent.getStringArrayExtra(EXTRA_NEEDED_PACKAGES)?.toSet().orEmpty(),
                leisurePackages = intent.getStringArrayExtra(EXTRA_LEISURE_PACKAGES)?.toSet().orEmpty(),
                reentryCooldownMillis = intent.getLongExtra(
                    EXTRA_REENTRY_COOLDOWN_MILLIS,
                    FocusMonitorCadence.REENTRY_COOLDOWN_MILLIS
                )
        )
        lastNotificationMillis = null
        lastStablePresence = null
        startForeground(ACTIVE_WORK_NOTIFICATION_ID, notifier.buildActiveWorkNotification(taskId, taskTitle))
        handler.removeCallbacks(monitorTick)
        handler.post(monitorTick)
    }

    private fun stopMonitoring() {
        handler.removeCallbacks(monitorTick)
        session = null
        if (::notifier.isInitialized) {
            notifier.clearReentryBanner()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun resetReentryCooldown(intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, INVALID_TASK_ID)
        if (session?.taskId == taskId) {
            lastNotificationMillis = null
        }
        if (session == null) {
            stopSelf()
        }
    }

    private fun scanForegroundApp() {
        val activeSession = session ?: return
        val nowMillis = System.currentTimeMillis()
        logPresence(activeSession, nowMillis)
        val foregroundPackage = usageStatsBoundary.latestForegroundPackage(
            sinceMillis = nowMillis - FocusMonitorCadence.USAGE_LOOKBACK_MILLIS,
            nowMillis = nowMillis
        )
        val decision = SoftLockPolicy.reentryDecision(
            activeWorkBlock = true,
            foregroundPackage = foregroundPackage,
            neededPackages = activeSession.neededPackages,
            leisurePackages = activeSession.leisurePackages,
            nowMillis = nowMillis,
            lastNotificationMillis = lastNotificationMillis,
            reentryCooldownMillis = activeSession.reentryCooldownMillis
        )
        if (decision.shouldNotify) {
            lastNotificationMillis = nowMillis
            notifier.showReentryBanner(activeSession.taskId, activeSession.taskTitle)
        }
    }

    private fun logPresence(activeSession: MonitorSession, nowMillis: Long) {
        val observation = foregroundObservationStore.read()
        val launcherPackages = launcherPackagesProvider.launcherPackages()
        val classifiedPresence = ForegroundPresenceClassifier.classify(
            attentionCoachInForeground = false,
            observation = observation,
            nowMillis = nowMillis,
            appPackage = packageName,
            whitelistPackages = activeSession.neededPackages,
            launcherPackages = launcherPackages
        )
        val presence = ForegroundPresenceMemory.resolve(
            classifiedPresence = classifiedPresence,
            lastStablePresence = lastStablePresence
        )
        if (classifiedPresence != FocusPresence.UNKNOWN) {
            lastStablePresence = classifiedPresence
        }
        val ageMillis = observation?.let { nowMillis - it.observedAtMillis }
        Log.d(
            PRESENCE_TAG,
            "rawPackage=${observation?.packageName} source=${observation?.source} " +
                "ageMillis=$ageMillis classified=$classifiedPresence presence=$presence " +
                "lastStable=$lastStablePresence launcherPackages=$launcherPackages"
        )
    }

    companion object {
        private const val ACTION_START = "com.example.attentioncoach.monitor.START"
        private const val ACTION_STOP = "com.example.attentioncoach.monitor.STOP"
        private const val ACTION_RESET_REENTRY_COOLDOWN = "com.example.attentioncoach.monitor.RESET_REENTRY_COOLDOWN"
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_TASK_TITLE = "task_title"
        private const val EXTRA_NEEDED_PACKAGES = "needed_packages"
        private const val EXTRA_LEISURE_PACKAGES = "leisure_packages"
        private const val EXTRA_REENTRY_COOLDOWN_MILLIS = "reentry_cooldown_millis"
        private const val INVALID_TASK_ID = -1L
        private const val ACTIVE_WORK_NOTIFICATION_ID = 4520
        private const val PRESENCE_TAG = "AC_PresenceV2"

        private val DEFAULT_LEISURE_PACKAGES = arrayOf(
            "com.google.android.youtube",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.aweme",
            "com.instagram.android"
        )

        fun start(
            context: Context,
            task: PlannedTask,
            neededPackages: Set<String>,
            reentryCooldownMillis: Long
        ) {
            val intent = Intent(context, FocusMonitorService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_TASK_TITLE, task.title)
                putExtra(EXTRA_NEEDED_PACKAGES, neededPackages.toTypedArray())
                putExtra(EXTRA_LEISURE_PACKAGES, DEFAULT_LEISURE_PACKAGES)
                putExtra(EXTRA_REENTRY_COOLDOWN_MILLIS, reentryCooldownMillis)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FocusMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun resetReentryCooldown(context: Context, taskId: Long) {
            val intent = Intent(context, FocusMonitorService::class.java).apply {
                action = ACTION_RESET_REENTRY_COOLDOWN
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }
    }
}

private data class MonitorSession(
    val taskId: Long,
    val taskTitle: String,
    val neededPackages: Set<String>,
    val leisurePackages: Set<String>,
    val reentryCooldownMillis: Long
)
