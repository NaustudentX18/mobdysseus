package com.jakemalby.odysseusmobile.persistence.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.Logger
import net.zetetic.database.NoopTarget
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [WorkspaceEntity::class, ConversationEntity::class, ChatMessageEntity::class, NoteEntity::class, TaskEntity::class, MemoryEntity::class, GalleryEntity::class],
    version = 1,
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
                .build()
        }

        private fun loadSqlCipher() {
            sqlCipherLoaded
        }

        private val sqlCipherLoaded: Unit by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            System.loadLibrary("sqlcipher")
            // Workspace content and database paths must never be copied to Logcat.
            Logger.setTarget(NoopTarget())
        }
    }
}
