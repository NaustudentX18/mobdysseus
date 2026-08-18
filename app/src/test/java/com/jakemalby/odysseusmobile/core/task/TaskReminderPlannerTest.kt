package com.jakemalby.odysseusmobile.core.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TaskReminderPlannerTest {
    private val brisbane = ZoneId.of("Australia/Brisbane")

    @Test
    fun oneOffReminderHonoursLeadTime() {
        val due = at(2026, 8, 20, 9, 0)
        val schedule = TaskSchedule("task-1", due, brisbane.id, remindBeforeMillis = 30 * 60_000L)

        val planned = TaskReminderPlanner.plan(
            schedule,
            nowInclusiveEpochMillis = at(2026, 8, 20, 8, 0),
            horizonExclusiveEpochMillis = at(2026, 8, 20, 10, 0),
        )

        assertEquals(1, planned.size)
        assertEquals(due, planned.single().dueAtEpochMillis)
        assertEquals(at(2026, 8, 20, 8, 30), planned.single().notifyAtEpochMillis)
    }

    @Test
    fun stableIdsDeduplicateExistingWork() {
        val due = at(2026, 8, 20, 9, 0)
        val schedule = TaskSchedule("task-1", due, brisbane.id)
        val first = TaskReminderPlanner.plan(schedule, due - 1, due + 1).single()

        val duplicate = TaskReminderPlanner.plan(
            schedule,
            due - 1,
            due + 1,
            existingReminderIds = setOf(first.reminderId),
        )

        assertTrue(duplicate.isEmpty())
        assertEquals(first.reminderId, TaskReminderPlanner.stableReminderId("task-1", due))
        assertNotEquals(first.reminderId, TaskReminderPlanner.stableReminderId("task-2", due))
    }

    @Test
    fun dailyRecurrencePreservesWallClockAcrossDst() {
        val sydney = ZoneId.of("Australia/Sydney")
        val firstDue = ZonedDateTime.of(2026, 10, 3, 9, 0, 0, 0, sydney).toInstant().toEpochMilli()
        val schedule = TaskSchedule("task-dst", firstDue, sydney.id, TaskRecurrence.DAILY)

        val planned = TaskReminderPlanner.plan(
            schedule,
            firstDue,
            ZonedDateTime.of(2026, 10, 6, 0, 0, 0, 0, sydney).toInstant().toEpochMilli(),
        )

        assertEquals(3, planned.size)
        val localTimes = planned.map { ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.dueAtEpochMillis), sydney) }
        assertTrue(localTimes.all { it.hour == 9 })
        assertEquals(23, (planned[1].dueAtEpochMillis - planned[0].dueAtEpochMillis) / 3_600_000L)
        assertEquals(24, (planned[2].dueAtEpochMillis - planned[1].dueAtEpochMillis) / 3_600_000L)
    }

    @Test
    fun oldRecurringScheduleFastForwardsIntoWindow() {
        val schedule = TaskSchedule(
            taskId = "old-task",
            dueAtEpochMillis = at(2020, 1, 1, 12, 0),
            zoneId = brisbane.id,
            recurrence = TaskRecurrence.MONTHLY,
        )

        val planned = TaskReminderPlanner.plan(
            schedule,
            nowInclusiveEpochMillis = at(2026, 8, 1, 0, 0),
            horizonExclusiveEpochMillis = at(2026, 11, 1, 0, 0),
        )

        assertEquals(3, planned.size)
        assertEquals(listOf(8, 9, 10), planned.map {
            ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.dueAtEpochMillis), brisbane).monthValue
        })
    }

    @Test
    fun monthlyRecurrenceRemainsAnchoredToOriginalDay() {
        val firstDue = at(2026, 1, 31, 9, 0)
        val schedule = TaskSchedule("month-end", firstDue, brisbane.id, TaskRecurrence.MONTHLY)

        val planned = TaskReminderPlanner.plan(
            schedule,
            firstDue,
            at(2026, 4, 1, 0, 0),
        )
        val localDates = planned.map {
            ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.dueAtEpochMillis), brisbane).toLocalDate()
        }

        assertEquals(listOf("2026-01-31", "2026-02-28", "2026-03-31"), localDates.map { it.toString() })
    }

    @Test
    fun disabledAndPastOneOffSchedulesProduceNothing() {
        val due = at(2026, 1, 1, 9, 0)
        val past = TaskSchedule("past", due, brisbane.id)
        val disabled = TaskSchedule("disabled", at(2027, 1, 1, 9, 0), brisbane.id, enabled = false)

        assertTrue(TaskReminderPlanner.plan(past, due + 1, due + 10_000).isEmpty())
        assertTrue(TaskReminderPlanner.plan(disabled, due, at(2028, 1, 1, 0, 0)).isEmpty())
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, brisbane).toInstant().toEpochMilli()
}
