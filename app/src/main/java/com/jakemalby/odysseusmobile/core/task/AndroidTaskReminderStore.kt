package com.jakemalby.odysseusmobile.core.task

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class ReminderDeliveryState {
    SCHEDULED,
    DELIVERED,
    NOTIFICATIONS_DENIED,
    DISMISSED,
    CANCELLED,
    FAILED,
}

data class StoredTaskReminder(
    val schedule: TaskSchedule,
    val title: String,
    val deliveryState: ReminderDeliveryState,
    val lastReminderId: String?,
    val updatedAtEpochMillis: Long,
)

/**
 * Plaintext JSON codec for [StoredTaskReminder]. Kept free of Android crypto so it can be
 * unit-tested on the JVM; [AndroidTaskReminderStore] wraps it with Keystore encryption.
 */
internal object TaskReminderCodec {
    fun encode(records: List<StoredTaskReminder>): String = JSONArray().apply {
        records.forEach { record ->
            put(JSONObject().apply {
                put("taskId", record.schedule.taskId)
                put("title", record.title)
                put("dueAt", record.schedule.dueAtEpochMillis)
                put("zoneId", record.schedule.zoneId)
                put("recurrence", record.schedule.recurrence.name)
                put("remindBefore", record.schedule.remindBeforeMillis)
                put("enabled", record.schedule.enabled)
                put("state", record.deliveryState.name)
                put("lastReminderId", record.lastReminderId ?: JSONObject.NULL)
                put("updatedAt", record.updatedAtEpochMillis)
            })
        }
    }.toString()

    fun decode(plain: String): List<StoredTaskReminder> {
        if (plain.isNullOrBlank()) return emptyList()
        val array = JSONArray(plain)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                runCatching {
                    StoredTaskReminder(
                        schedule = TaskSchedule(
                            taskId = item.getString("taskId"),
                            dueAtEpochMillis = item.getLong("dueAt"),
                            zoneId = item.getString("zoneId"),
                            recurrence = TaskRecurrence.valueOf(item.optString("recurrence", TaskRecurrence.NONE.name)),
                            remindBeforeMillis = item.optLong("remindBefore", 0),
                            enabled = item.optBoolean("enabled", true),
                        ),
                        title = item.getString("title"),
                        deliveryState = ReminderDeliveryState.valueOf(item.optString("state", ReminderDeliveryState.SCHEDULED.name)),
                        lastReminderId = item.optString("lastReminderId").takeIf { !item.isNull("lastReminderId") && it.isNotBlank() },
                        updatedAtEpochMillis = item.optLong("updatedAt", 0),
                    )
                }.getOrNull()?.let(::add)
            }
        }
    }
}

/** App-private, Keystore-encrypted schedule store, independent from the workspace schema. */
class AndroidTaskReminderStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun all(): List<StoredTaskReminder> = TaskReminderCodec.decode(ReminderStoreCipher.decrypt(preferences.getString(PAYLOAD, null)))

    @Synchronized
    fun get(taskId: String): StoredTaskReminder? = all().firstOrNull { it.schedule.taskId == taskId }

    @Synchronized
    fun put(record: StoredTaskReminder) {
        val records = all().filterNot { it.schedule.taskId == record.schedule.taskId } + record
        persist(records)
    }

    @Synchronized
    fun updateDelivery(
        taskId: String,
        state: ReminderDeliveryState,
        reminderId: String?,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ) {
        val records = all().map { record ->
            if (record.schedule.taskId == taskId &&
                (reminderId == null || record.lastReminderId == null || record.lastReminderId == reminderId)
            ) record.copy(deliveryState = state, lastReminderId = reminderId, updatedAtEpochMillis = nowEpochMillis)
            else record
        }
        persist(records)
    }

    @Synchronized
    fun remove(taskId: String) {
        persist(all().filterNot { it.schedule.taskId == taskId })
    }

    private fun persist(records: List<StoredTaskReminder>) {
        check(preferences.edit().putString(PAYLOAD, ReminderStoreCipher.encrypt(TaskReminderCodec.encode(records))).commit()) {
            "Task reminder schedules could not be saved"
        }
    }

    companion object {
        private const val PREFS = "mobdysseus.task_reminders.v1"
        private const val PAYLOAD = "encrypted_schedules"
    }
}

private object ReminderStoreCipher {
    private const val KEY_ALIAS = "mobdysseus.task_reminders.aes.v1"
    private const val PREFIX = "mdtr1:"

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return PREFIX + Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(value: String?): String {
        require(value != null && value.startsWith(PREFIX)) { "Task reminder storage is invalid" }
        val payload = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
        require(payload.size > 12) { "Task reminder storage is invalid" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
        return String(cipher.doFinal(payload.copyOfRange(12, payload.size)), StandardCharsets.UTF_8)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }
}
