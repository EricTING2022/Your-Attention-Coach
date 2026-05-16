package com.example.attentioncoach.platform

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.example.attentioncoach.domain.FocusMonitorCadence
import com.example.attentioncoach.domain.ForegroundPresenceClassifier
import com.example.attentioncoach.domain.ForegroundPresenceMemory
import com.example.attentioncoach.domain.FocusPresence
import com.example.attentioncoach.domain.FocusMonitorLoopPolicy
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.PresenceReentryPolicy
import com.example.attentioncoach.domain.ReentryReason
import com.example.attentioncoach.domain.ScreenOffReentryPolicy

class FocusMonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var foregroundObservationStore: ForegroundObservationStore
    private lateinit var launcherPackagesProvider: LauncherPackagesProvider
    private lateinit var reentryMonitorStateStore: ReentryMonitorStateStore
    private lateinit var notifier: ReentryNotifier
    private var session: MonitorSession? = null
    private var lastNotificationMillis: Long? = null
    private var violationStartedAtMillis: Long? = null
    private var lastStablePresence: FocusPresence? = null
    private var lastScheduledScreenOffAtMillis: Long? = null
    private var monitorPollingPausedForScreenOff = false

    private val monitorTick = object : Runnable {
        override fun run() {
            if (monitorPollingPausedForScreenOff) return
            scanForegroundApp()
            if (!monitorPollingPausedForScreenOff) {
                handler.postDelayed(this, FocusMonitorCadence.POLL_INTERVAL_MILLIS)
            }
        }
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (session != null) {
                        handler.removeCallbacks(monitorTick)
                        scanForegroundApp()
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (session != null) {
                        resumeMonitorPolling()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        foregroundObservationStore = ForegroundObservationStore(this)
        launcherPackagesProvider = LauncherPackagesProvider(this)
        reentryMonitorStateStore = ReentryMonitorStateStore(this)
        notifier = ReentryNotifier(this)
        notifier.ensureChannels()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(screenStateReceiver, filter)
        }
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
        runCatching { unregisterReceiver(screenStateReceiver) }
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
        violationStartedAtMillis = null
        lastStablePresence = null
        lastScheduledScreenOffAtMillis = null
        monitorPollingPausedForScreenOff = false
        startForeground(ACTIVE_WORK_NOTIFICATION_ID, notifier.buildActiveWorkNotification(taskId, taskTitle))
        resumeMonitorPolling()
    }

    private fun stopMonitoring() {
        handler.removeCallbacks(monitorTick)
        session = null
        if (::reentryMonitorStateStore.isInitialized) {
            reentryMonitorStateStore.clear()
        }
        ReentryReminderReceiver.cancel(this)
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
            violationStartedAtMillis = null
            reentryMonitorStateStore.clear()
            ReentryReminderReceiver.cancel(this)
            notifier.clearReentryBanner()
            if (isDeviceInteractive()) {
                resumeMonitorPolling()
            } else {
                monitorPollingPausedForScreenOff = false
            }
        }
        if (session == null) {
            stopSelf()
        }
    }

    private fun scanForegroundApp() {
        val activeSession = session ?: return
        val nowMillis = System.currentTimeMillis()
        val presence = resolvePresence(activeSession, nowMillis)
        persistMonitorState(activeSession, presence)
        if (!isDeviceInteractive()) {
            handleScreenOff(activeSession, presence, nowMillis)
            if (FocusMonitorLoopPolicy.shouldPausePolling(deviceInteractive = false)) {
                pauseMonitorPollingForScreenOff()
            }
            return
        }
        ReentryReminderReceiver.cancel(this)
        lastScheduledScreenOffAtMillis = null
        val decision = PresenceReentryPolicy.screenOnDecision(
            activeWorkBlock = true,
            presence = presence,
            nowMillis = nowMillis,
            violationStartedAtMillis = violationStartedAtMillis,
            lastNotificationMillis = lastNotificationMillis,
            reentryCooldownMillis = activeSession.reentryCooldownMillis
        )
        violationStartedAtMillis = decision.nextViolationStartedAtMillis
        lastNotificationMillis = decision.nextLastNotificationMillis
        Log.d(
            REENTRY_TAG,
            "presence=$presence reason=${decision.reason} shouldNotify=${decision.shouldNotify} " +
                "shouldClear=${decision.shouldClearNotification} violationStarted=$violationStartedAtMillis " +
                "lastNotification=$lastNotificationMillis"
        )
        if (decision.shouldClearNotification) {
            notifier.clearReentryBanner()
            reentryMonitorStateStore.clear()
        }
        if (decision.shouldNotify) {
            notifier.showReentryBanner(activeSession.taskId, activeSession.taskTitle)
        }
        persistMonitorState(activeSession, presence)
    }

    private fun handleScreenOff(
        activeSession: MonitorSession,
        presence: FocusPresence,
        nowMillis: Long
    ) {
        val decision = ScreenOffReentryPolicy.alarmDecision(
            activeWorkBlock = true,
            presence = presence,
            nowMillis = nowMillis,
            violationStartedAtMillis = violationStartedAtMillis,
            lastNotificationMillis = lastNotificationMillis,
            reentryCooldownMillis = activeSession.reentryCooldownMillis
        )
        violationStartedAtMillis = decision.nextViolationStartedAtMillis
        lastNotificationMillis = decision.nextLastNotificationMillis
        Log.d(
            REENTRY_ALARM_TAG,
            "screenOff presence=$presence reason=${decision.reason} " +
                "schedule=${decision.shouldScheduleAlarm} clear=${decision.shouldClearAlarm} " +
                "delay=${decision.delayMillis} violationStarted=$violationStartedAtMillis " +
                "lastNotification=$lastNotificationMillis"
        )
        if (decision.shouldClearAlarm) {
            ReentryReminderReceiver.cancel(this)
            lastScheduledScreenOffAtMillis = null
            notifier.clearReentryBanner()
            reentryMonitorStateStore.clear()
        } else if (decision.shouldScheduleAlarm) {
            val triggerAtMillis = screenOffTriggerAtMillis(decision.reason, decision.delayMillis)
            if (lastScheduledScreenOffAtMillis != triggerAtMillis) {
                ReentryReminderReceiver.schedule(this, decision.delayMillis)
                lastScheduledScreenOffAtMillis = triggerAtMillis
            }
        }
        persistMonitorState(activeSession, presence)
    }

    private fun screenOffTriggerAtMillis(reason: ReentryReason, delayMillis: Long): Long {
        return when (reason) {
            ReentryReason.GRACE_PERIOD ->
                (violationStartedAtMillis ?: System.currentTimeMillis()) + FocusMonitorCadence.REENTRY_GRACE_MILLIS
            ReentryReason.COOLDOWN ->
                (lastNotificationMillis ?: System.currentTimeMillis()) + (session?.reentryCooldownMillis ?: FocusMonitorCadence.REENTRY_COOLDOWN_MILLIS)
            else -> System.currentTimeMillis() + delayMillis
        }
    }

    private fun resumeMonitorPolling() {
        monitorPollingPausedForScreenOff = false
        handler.removeCallbacks(monitorTick)
        handler.post(monitorTick)
    }

    private fun pauseMonitorPollingForScreenOff() {
        monitorPollingPausedForScreenOff = true
        handler.removeCallbacks(monitorTick)
    }

    private fun resolvePresence(activeSession: MonitorSession, nowMillis: Long): FocusPresence {
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
        return presence
    }

    private fun isDeviceInteractive(): Boolean {
        return getSystemService(PowerManager::class.java)?.isInteractive ?: true
    }

    private fun persistMonitorState(
        activeSession: MonitorSession,
        presence: FocusPresence
    ) {
        reentryMonitorStateStore.save(
            ReentryMonitorState(
                active = true,
                taskId = activeSession.taskId,
                taskTitle = activeSession.taskTitle,
                presence = presence,
                violationStartedAtMillis = violationStartedAtMillis,
                lastNotificationMillis = lastNotificationMillis,
                reentryCooldownMillis = activeSession.reentryCooldownMillis,
                screenOffFullScreenShownForViolation = reentryMonitorStateStore.read()
                    ?.screenOffFullScreenShownForViolation
                    ?: false
            )
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
        private const val REENTRY_TAG = "AC_ReentryV2"
        private const val REENTRY_ALARM_TAG = "AC_ReentryAlarmV2"

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
