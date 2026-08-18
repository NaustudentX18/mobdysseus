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
    version = 2,
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
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        private fun loadSqlCipher() {
            sqlCipherLoaded
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
