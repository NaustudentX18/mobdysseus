package com.jakemalby.odysseusmobile.core.task

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

enum class TaskRecurrence {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
}

/**
 * Persistence-independent scheduling contract for a task reminder.
 *
 * [dueAtEpochMillis] is interpreted in [zoneId] before recurrence is applied. This preserves the
 * user's chosen local wall-clock time over daylight-saving transitions.
 */
data class TaskSchedule(
    val taskId: String,
    val dueAtEpochMillis: Long,
    val zoneId: String,
    val recurrence: TaskRecurrence = TaskRecurrence.NONE,
    val remindBeforeMillis: Long = 0,
    val enabled: Boolean = true,
) {
    init {
        require(taskId.isNotBlank()) { "taskId must not be blank" }
        require(remindBeforeMillis >= 0) { "remindBeforeMillis must not be negative" }
        ZoneId.of(zoneId)
    }
}

data class PlannedTaskReminder(
    val reminderId: String,
    val taskId: String,
    val dueAtEpochMillis: Long,
    val notifyAtEpochMillis: Long,
)

/** Pure planner; Android notification and WorkManager adapters can consume its output. */
object TaskReminderPlanner {
    private const val ID_NAMESPACE = "mobdysseus-task-reminder:v1"

    fun plan(
        schedule: TaskSchedule,
        nowInclusiveEpochMillis: Long,
        horizonExclusiveEpochMillis: Long,
        existingReminderIds: Set<String> = emptySet(),
        maximumOccurrences: Int = 128,
    ): List<PlannedTaskReminder> {
        require(horizonExclusiveEpochMillis >= nowInclusiveEpochMillis) {
            "horizon must not be before now"
        }
        require(maximumOccurrences > 0) { "maximumOccurrences must be positive" }
        if (!schedule.enabled || horizonExclusiveEpochMillis == nowInclusiveEpochMillis) {
            return emptyList()
        }

        val zone = ZoneId.of(schedule.zoneId)
        val originalDue = Instant.ofEpochMilli(schedule.dueAtEpochMillis).atZone(zone)
        var occurrence = fastForward(
            originalDue,
            schedule.recurrence,
            nowInclusiveEpochMillis - schedule.remindBeforeMillis,
        )
        val planned = mutableListOf<PlannedTaskReminder>()

        while (planned.size < maximumOccurrences) {
            val due = occurrence.due
            val dueMillis = due.toInstant().toEpochMilli()
            val notifyMillis = dueMillis - schedule.remindBeforeMillis
            if (notifyMillis >= horizonExclusiveEpochMillis) break

            if (notifyMillis >= nowInclusiveEpochMillis) {
                val reminderId = stableReminderId(schedule.taskId, dueMillis)
                if (reminderId !in existingReminderIds) {
                    planned += PlannedTaskReminder(
                        reminderId = reminderId,
                        taskId = schedule.taskId,
                        dueAtEpochMillis = dueMillis,
                        notifyAtEpochMillis = notifyMillis,
                    )
                }
            }

            if (schedule.recurrence == TaskRecurrence.NONE) break
            occurrence = occurrence.copy(
                index = occurrence.index + 1,
                due = originalDue.advance(schedule.recurrence, occurrence.index + 1),
            )
        }
        return planned
    }

    fun stableReminderId(taskId: String, dueAtEpochMillis: Long): String =
        UUID.nameUUIDFromBytes(
            "$ID_NAMESPACE:$taskId:$dueAtEpochMillis".toByteArray(StandardCharsets.UTF_8),
        ).toString()

    private fun fastForward(
        originalDue: ZonedDateTime,
        recurrence: TaskRecurrence,
        earliestDueEpochMillis: Long,
    ): Occurrence {
        if (recurrence == TaskRecurrence.NONE || originalDue.toInstant().toEpochMilli() >= earliestDueEpochMillis) {
            return Occurrence(0, originalDue)
        }
        val earliest = Instant.ofEpochMilli(earliestDueEpochMillis).atZone(originalDue.zone)
        val approximateSteps = when (recurrence) {
            TaskRecurrence.DAILY -> ChronoUnit.DAYS.between(originalDue.toLocalDate(), earliest.toLocalDate())
            TaskRecurrence.WEEKLY -> ChronoUnit.WEEKS.between(originalDue.toLocalDate(), earliest.toLocalDate())
            TaskRecurrence.MONTHLY -> ChronoUnit.MONTHS.between(
                originalDue.toLocalDate().withDayOfMonth(1),
                earliest.toLocalDate().withDayOfMonth(1),
            )
            TaskRecurrence.NONE -> 0
        }.coerceAtLeast(0)

        var index = approximateSteps
        var candidate = originalDue.advance(recurrence, index)
        while (candidate.toInstant().toEpochMilli() < earliestDueEpochMillis) {
            index += 1
            candidate = originalDue.advance(recurrence, index)
        }
        return Occurrence(index, candidate)
    }

    private fun ZonedDateTime.advance(recurrence: TaskRecurrence, amount: Long): ZonedDateTime =
        when (recurrence) {
            TaskRecurrence.NONE -> this
            TaskRecurrence.DAILY -> plusDays(amount)
            TaskRecurrence.WEEKLY -> plusWeeks(amount)
            TaskRecurrence.MONTHLY -> plusMonths(amount)
        }

    private data class Occurrence(val index: Long, val due: ZonedDateTime)
}
