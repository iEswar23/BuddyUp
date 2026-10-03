package io.github.ieswar23.buddyup.data

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.buddyup.data.local.BuddyUpDatabase
import io.github.ieswar23.buddyup.data.local.entity.BlockedPersonEntity
import io.github.ieswar23.buddyup.domain.model.ConnectionState
import io.github.ieswar23.buddyup.domain.model.ReportReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Builds a version-1 database by hand (the exact schema Room generated for v1), then opens it with
 * the current [BuddyUpDatabase]. Room validates the migrated schema against the entities, so a
 * mistake in [BuddyUpDatabase.MIGRATION_1_2] fails this test instead of wiping users' data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun `migrating from v1 to v2 keeps friends and chats and adds blocking`() = runBlocking<Unit> {
        createVersion1Database()

        val db = Room.databaseBuilder(context, BuddyUpDatabase::class.java, dbName)
            .addMigrations(*BuddyUpDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            assertThat(db.openHelper.writableDatabase.version).isEqualTo(2)
            assertThat(db.connectionDao().get("p03")?.state).isEqualTo(ConnectionState.FRIEND)
            assertThat(db.messageDao().recent("p03", limit = 10).map { it.text }).containsExactly("See you Saturday?")

            db.blockDao().upsert(BlockedPersonEntity("p03", ReportReason.SPAM, note = "Selling courses", blockedAt = 42L))
            assertThat(db.blockDao().isBlocked("p03")).isTrue()
            assertThat(db.blockDao().observeBlocked().first().single().block.note).isEqualTo("Selling courses")
            assertThat(db.connectionDao().observeFriends().first()).isEmpty()
        } finally {
            db.close()
        }
    }

    private fun createVersion1Database() {
        val file = context.getDatabasePath(dbName).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            VERSION_1_SCHEMA.forEach(db::execSQL)
            db.execSQL(
                "INSERT INTO people VALUES ('p03', 'Kavya Iyer', 27, 'Bengaluru', 'Koramangala', 12.93, 77.62, " +
                    "'Editor', 'Bookworm', 'Reading|Coffee', 0, 0)"
            )
            db.execSQL("INSERT INTO connections VALUES ('p03', 'FRIEND', NULL, 0, 0)")
            db.execSQL("INSERT INTO messages (friendId, text, sentAt, fromMe, isRead) VALUES ('p03', 'See you Saturday?', 0, 0, 1)")
            db.version = 1
        }
    }

    private companion object {
        /** Copied from the Room-generated `BuddyUpDatabase_Impl` for schema version 1. */
        val VERSION_1_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `people` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `age` INTEGER NOT NULL, `city` TEXT NOT NULL, `neighborhood` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `occupation` TEXT NOT NULL, `bio` TEXT NOT NULL, `interests` TEXT NOT NULL, `lastActiveAt` INTEGER NOT NULL, `wavesBack` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `connections` (`personId` TEXT NOT NULL, `state` TEXT NOT NULL, `note` TEXT, `updatedAt` INTEGER NOT NULL, `friendsSince` INTEGER, PRIMARY KEY(`personId`))",
            "CREATE TABLE IF NOT EXISTS `messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `friendId` TEXT NOT NULL, `text` TEXT NOT NULL, `sentAt` INTEGER NOT NULL, `fromMe` INTEGER NOT NULL, `isRead` INTEGER NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_messages_friendId` ON `messages` (`friendId`)",
            "CREATE INDEX IF NOT EXISTS `index_messages_sentAt` ON `messages` (`sentAt`)",
            "CREATE TABLE IF NOT EXISTS `meetups` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `emoji` TEXT NOT NULL, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `city` TEXT NOT NULL, `venue` TEXT NOT NULL, `host` TEXT NOT NULL, `startsAt` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `attendeeCount` INTEGER NOT NULL, `capacity` INTEGER NOT NULL, `isJoined` INTEGER NOT NULL, `tags` TEXT NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e47a5594df1685a4df89126eb2a8fc68')",
        )
    }
}
