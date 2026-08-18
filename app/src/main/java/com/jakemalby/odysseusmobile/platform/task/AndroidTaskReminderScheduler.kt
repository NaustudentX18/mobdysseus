package com.jakemalby.odysseusmobile.platform.task

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.jakemalby.odysseusmobile.MainActivity
import com.jakemalby.odysseusmobile.R
import com.jakemalby.odysseusmobile.core.task.AndroidTaskReminderStore
import com.jakemalby.odysseusmobile.core.task.ReminderDeliveryState
import com.jakemalby.odysseusmobile.core.task.StoredTaskReminder
import com.jakemalby.odysseusmobile.core.task.TaskRecurrence
import com.jakemalby.odysseusmobile.core.task.TaskReminderPlanner
import com.jakemalby.odysseusmobile.core.task.TaskReminderScheduler
import com.jakemalby.odysseusmobile.core.task.TaskSchedule
import java.util.concurrent.TimeUnit

class AndroidTaskReminderScheduler(context: Context) : TaskReminderScheduler {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val store = AndroidTaskReminderStore(appContext)

    override fun schedule(schedule: TaskSchedule, title: String, nowEpochMillis: Long) {
        workManager.cancelAllWorkByTag(taskTag(schedule.taskId))
        enqueue(schedule, title, nowEpochMillis, storeRecord = true)
    }

    override fun cancel(taskId: String) {
        workManager.cancelAllWorkByTag(taskTag(taskId))
        store.remove(taskId)
    }

    internal fun enqueueNext(schedule: TaskSchedule, title: String, afterEpochMillis: Long) {
        enqueue(schedule, title, afterEpochMillis, storeRecord = false)
    }

    private fun enqueue(schedule: TaskSchedule, title: String, nowEpochMillis: Long, storeRecord: Boolean) {
        val reminder = TaskReminderPlanner.plan(
            schedule = schedule,
            nowInclusiveEpochMillis = nowEpochMillis,
            horizonExclusiveEpochMillis = Long.MAX_VALUE,
            maximumOccurrences = 1,
        ).firstOrNull()

        if (reminder == null) {
            if (storeRecord) store.remove(schedule.taskId)
            return
        }
        val record = StoredTaskReminder(
            schedule = schedule,
            title = title.trim().ifEmpty { "Task reminder" },
            deliveryState = ReminderDeliveryState.SCHEDULED,
            lastReminderId = reminder.reminderId,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        store.put(record)
        val input = Data.Builder()
            .putString(TaskReminderWorker.TASK_ID, schedule.taskId)
            .putString(TaskReminderWorker.REMINDER_ID, reminder.reminderId)
            .putLong(TaskReminderWorker.DUE_AT, reminder.dueAtEpochMillis)
            .build()
        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay((reminder.notifyAtEpochMillis - nowEpochMillis).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(input)
            .addTag(taskTag(schedule.taskId))
            .build()
        workManager.enqueueUniqueWork(workName(reminder.reminderId), ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        internal fun taskTag(taskId: String) = "mobdysseus.task.$taskId"
        internal fun workName(reminderId: String) = "mobdysseus.reminder.$reminderId"
    }
}

class TaskReminderWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        val taskId = inputData.getString(TASK_ID) ?: return Result.failure()
        val reminderId = inputData.getString(REMINDER_ID) ?: return Result.failure()
        val dueAt = inputData.getLong(DUE_AT, -1)
        val store = AndroidTaskReminderStore(applicationContext)
        val record = store.get(taskId) ?: return Result.success()
        if (record.lastReminderId != reminderId ||
            TaskReminderPlanner.stableReminderId(taskId, dueAt) != reminderId
        ) return Result.success()

        val notificationsAllowed = NotificationManagerCompat.from(applicationContext).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED)

        if (notificationsAllowed) {
            TaskReminderNotifications.show(applicationContext, record, reminderId)
            store.updateDelivery(taskId, ReminderDeliveryState.DELIVERED, reminderId)
        } else {
            store.updateDelivery(taskId, ReminderDeliveryState.NOTIFICATIONS_DENIED, reminderId)
        }

        if (record.schedule.recurrence != TaskRecurrence.NONE) {
            AndroidTaskReminderScheduler(applicationContext).enqueueNext(record.schedule, record.title, dueAt + 1)
        }
        return Result.success()
    }

    companion object {
        const val TASK_ID = "task_id"
        const val REMINDER_ID = "reminder_id"
        const val DUE_AT = "due_at"
    }
}

object TaskReminderNotifications {
    private const val CHANNEL_ID = "mobdysseus_task_reminders"

    fun show(context: Context, record: StoredTaskReminder, reminderId: String) {
        createChannel(context)
        val taskId = record.schedule.taskId
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("mobdysseus://tasks/${Uri.encode(taskId)}")
            putExtra(TaskReminderWorker.TASK_ID, taskId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val dismissIntent = Intent(context, TaskReminderActionReceiver::class.java).apply {
            action = TaskReminderActionReceiver.ACTION_DISMISSED
            putExtra(TaskReminderWorker.TASK_ID, taskId)
            putExtra(TaskReminderWorker.REMINDER_ID, reminderId)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_odysseus)
            .setContentTitle("Task reminder")
            .setContentText(record.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(record.title))
            .setContentIntent(PendingIntent.getActivity(context, reminderId.hashCode(), openIntent, immutableUpdateFlags()))
            .setDeleteIntent(PendingIntent.getBroadcast(context, reminderId.hashCode(), dismissIntent, immutableUpdateFlags()))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Task reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Private reminders for Mobdysseus tasks"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun immutableUpdateFlags() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}

class TaskReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISMISSED) return
        val taskId = intent.getStringExtra(TaskReminderWorker.TASK_ID) ?: return
        val reminderId = intent.getStringExtra(TaskReminderWorker.REMINDER_ID) ?: return
        AndroidTaskReminderStore(context).updateDelivery(taskId, ReminderDeliveryState.DISMISSED, reminderId)
    }

    companion object {
        const val ACTION_DISMISSED = "com.jakemalby.odysseusmobile.action.REMINDER_DISMISSED"
    }
}
