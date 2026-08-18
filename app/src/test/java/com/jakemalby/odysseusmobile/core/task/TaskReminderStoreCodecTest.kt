package com.jakemalby.odysseusmobile.core.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskReminderStoreCodecTest {
    @Test
    fun schedulesAndDeliveryStateRoundTrip() {
        val expected = StoredTaskReminder(
            schedule = TaskSchedule(
                taskId = "task-7",
                dueAtEpochMillis = 1_800_000_000_000,
                zoneId = "Australia/Brisbane",
                recurrence = TaskRecurrence.WEEKLY,
                remindBeforeMillis = 900_000,
            ),
            title = "Review private notes",
            deliveryState = ReminderDeliveryState.NOTIFICATIONS_DENIED,
            lastReminderId = "stable-id",
            updatedAtEpochMillis = 1_700_000_000_000,
        )

        assertEquals(listOf(expected), TaskReminderCodec.decode(TaskReminderCodec.encode(listOf(expected))))
    }

    @Test
    fun missingOrCorruptRecordsDoNotDestroyValidRecords() {
        assertTrue(TaskReminderCodec.decode("").isEmpty())
        val encoded = """[{"taskId":"bad","title":"Bad","dueAt":1,"zoneId":"No/SuchZone"},{"taskId":"ok","title":"Okay","dueAt":1800000000000,"zoneId":"UTC","state":"SCHEDULED","updatedAt":4}]"""

        val decoded = TaskReminderCodec.decode(encoded)

        assertEquals(listOf("ok"), decoded.map { it.schedule.taskId })
    }
}
