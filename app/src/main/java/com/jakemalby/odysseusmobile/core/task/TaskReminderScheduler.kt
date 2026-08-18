package com.jakemalby.odysseusmobile.core.task

/**
 * Platform-neutral reminder scheduling contract. Features depend on this
 * interface; the app shell wires the Android/WorkManager implementation.
 */
interface TaskReminderScheduler {
    fun schedule(schedule: TaskSchedule, title: String, nowEpochMillis: Long = System.currentTimeMillis())
    fun cancel(taskId: String)
}
