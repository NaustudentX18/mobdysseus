package com.jakemalby.odysseusmobile.persistence.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.Logger
import net.zetetic.database.NoopTarget
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [WorkspaceEntity::class, ConversationEntity::class, ChatMessageEntity::class, NoteEntity::class, TaskEntity::class, MemoryEntity::class, GalleryEntity::class],
    version = 5,
    exportSchema = true,
)
internal abstract class MobdysseusDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao

    companion object {
        private const val DATABASE_NAME = "mobdysseus-encrypted.db"

        fun create(context: Context): MobdysseusDatabase {
            val appContext = context.applicationContext
            loadSqlCipher()
            val passphrase = EncryptedDatabaseKeyStore(appContext).loadOrCreate(
                databaseExists = appContext.getDatabasePath(DATABASE_NAME).exists(),
            )
            return Room.databaseBuilder(appContext, MobdysseusDatabase::class.java, DATABASE_NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase, null, true))
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
        }

        private fun loadSqlCipher() {
            sqlCipherLoaded
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workspace ADD COLUMN temperature REAL NOT NULL DEFAULT 0.7")
                db.execSQL("ALTER TABLE workspace ADD COLUMN topP REAL NOT NULL DEFAULT 0.9")
                db.execSQL("ALTER TABLE workspace ADD COLUMN topK INTEGER NOT NULL DEFAULT 32")
                db.execSQL("ALTER TABLE workspace ADD COLUMN maxTokens INTEGER NOT NULL DEFAULT 2048")
                db.execSQL("ALTER TABLE workspace ADD COLUMN systemPrompt TEXT NOT NULL DEFAULT 'You are Mobdysseus, a private, concise assistant running entirely on this Android phone.'")
                db.execSQL("ALTER TABLE workspace ADD COLUMN ragTopK INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE workspace ADD COLUMN voiceAutoSpeak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workspace ADD COLUMN voiceSpeechRate REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE workspace ADD COLUMN voiceSpeechPitch REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE workspace ADD COLUMN biometricLockEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workspace ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE workspace ADD COLUMN markdownPreviewDefault INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE workspace ADD COLUMN autoSaveDrafts INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workspace ADD COLUMN theme TEXT NOT NULL DEFAULT 'OBSIDIAN_CORAL'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE note ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE task ADD COLUMN dueAt INTEGER")
                db.execSQL("ALTER TABLE task ADD COLUMN recurrence TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE task ADD COLUMN remindBeforeMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val sqlCipherLoaded: Unit by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            System.loadLibrary("sqlcipher")
            // Workspace content and database paths must never be copied to Logcat.
            Logger.setTarget(NoopTarget())
        }
    }
}
