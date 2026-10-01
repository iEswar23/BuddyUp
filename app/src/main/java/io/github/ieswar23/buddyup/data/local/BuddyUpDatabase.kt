package io.github.ieswar23.buddyup.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.ieswar23.buddyup.data.local.dao.ConnectionDao
import io.github.ieswar23.buddyup.data.local.dao.MeetupDao
import io.github.ieswar23.buddyup.data.local.dao.MessageDao
import io.github.ieswar23.buddyup.data.local.dao.PersonDao
import io.github.ieswar23.buddyup.data.local.entity.ConnectionEntity
import io.github.ieswar23.buddyup.data.local.entity.MeetupEntity
import io.github.ieswar23.buddyup.data.local.entity.MessageEntity
import io.github.ieswar23.buddyup.data.local.entity.PersonEntity

@Database(
    entities = [PersonEntity::class, ConnectionEntity::class, MessageEntity::class, MeetupEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class BuddyUpDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun connectionDao(): ConnectionDao
    abstract fun messageDao(): MessageDao
    abstract fun meetupDao(): MeetupDao

    companion object {
        const val NAME = "buddyup.db"
    }
}
