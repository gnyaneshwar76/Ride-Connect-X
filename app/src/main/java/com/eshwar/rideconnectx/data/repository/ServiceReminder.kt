package com.eshwar.rideconnectx.data.repository

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.eshwar.rideconnectx.MainActivity
import com.eshwar.rideconnectx.core.di.ApplicationScope
import com.eshwar.rideconnectx.core.di.ServiceEntryPoint
import com.eshwar.rideconnectx.data.local.ServicePreferencesStore
import com.eshwar.rideconnectx.data.local.db.NotificationKind
import com.eshwar.rideconnectx.domain.model.ServiceReminderRule
import com.eshwar.rideconnectx.domain.model.ServiceStatus
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Raises service reminders: one in-app Notifications entry and one phone
 * notification per due-soon or overdue service (see [ServiceReminderRule]).
 *
 * Nothing did this before — the Service screen said "Overdue — 300 days over"
 * while the Notifications list stayed empty and the phone stayed silent
 * (rider, 26 Sep). Checked whenever the status changes while the app runs
 * (records added or edited, odometer, account switch) and once a day by
 * [ServiceReminderWorker] while it doesn't.
 */
@Singleton
class ServiceReminder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val service: ServiceRepository,
    private val prefs: ServicePreferencesStore,
    private val notifications: NotificationRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val lock = Mutex()
    private var started = false

    fun start() {
        if (started) return
        started = true
        appScope.launch {
            combine(service.ownedStatus, service.remindersEnabled) { owned, on -> Triple(owned.first, owned.second, on) }
                .collect { (owner, status, on) -> check(owner, status, on) }
        }
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ServiceReminderWorker>(1, TimeUnit.DAYS).build(),
        )
    }

    suspend fun checkOnce() {
        val (owner, status) = service.ownedStatus.first()
        check(owner, status, service.remindersEnabled.first())
    }

    private suspend fun check(owner: String, status: ServiceStatus, enabled: Boolean) = lock.withLock {
        val key = ServiceReminderRule.reminderKey(status, enabled)
        if (!ServiceReminderRule.shouldNotify(key, prefs.lastReminder(owner))) return@withLock
        // Recorded first: a second check racing this one must not post it again.
        prefs.setLastReminder(owner, key!!)
        val title = if (status.isOverdue) "Service overdue" else "Service due soon"
        val body = listOf(
            if (status.isOverdue) "Your scooter is due for a service." else "Your next service is coming up.",
            ServiceReminderRule.detail(status),
        ).filter { it.isNotBlank() }.joinToString(" ")
        Log.d(TAG, "Reminder $key for $owner")
        notifications.notify(NotificationKind.SERVICE, title, body, ownerId = owner)
        postToPhone(title, body)
    }

    /** Skipped quietly without the permission; the Notifications page warns (N7). */
    private fun postToPhone(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "Service reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    private companion object {
        const val TAG = "RCX-Service"
        const val CHANNEL = "service_reminders"
        const val NOTIFICATION_ID = 2
        const val WORK_NAME = "service-reminder"
    }
}

/** The once-a-day check for when the app is not open. */
class ServiceReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        EntryPointAccessors.fromApplication(applicationContext, ServiceEntryPoint::class.java)
            .serviceReminder().checkOnce()
        return Result.success()
    }
}
