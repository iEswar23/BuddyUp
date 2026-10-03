package io.github.ieswar23.buddyup.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.ieswar23.buddyup.data.local.dao.BlockDao
import io.github.ieswar23.buddyup.data.local.dao.ConnectionDao
import io.github.ieswar23.buddyup.data.local.dao.MeetupDao
import io.github.ieswar23.buddyup.data.local.dao.MessageDao
import io.github.ieswar23.buddyup.data.local.dao.PersonDao
import io.github.ieswar23.buddyup.data.local.entity.BlockedPersonEntity
import io.github.ieswar23.buddyup.data.local.entity.ConnectionEntity
import io.github.ieswar23.buddyup.data.local.entity.MeetupEntity
import io.github.ieswar23.buddyup.data.local.entity.MessageEntity
import io.github.ieswar23.buddyup.data.local.entity.PersonEntity

@Database(
    entities = [
        PersonEntity::class,
        ConnectionEntity::class,
        MessageEntity::class,
        MeetupEntity::class,
        BlockedPersonEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class BuddyUpDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun connectionDao(): ConnectionDao
    abstract fun messageDao(): MessageDao
    abstract fun meetupDao(): MeetupDao
    abstract fun blockDao(): BlockDao

    companion object {
        const val NAME = "buddyup.db"

        /** v2 adds the `blocked_people` table; existing friends, waves and chats are kept. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `blocked_people` (`personId` TEXT NOT NULL, `reason` TEXT, " +
                        "`note` TEXT, `blockedAt` INTEGER NOT NULL, PRIMARY KEY(`personId`))"
                )
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}
