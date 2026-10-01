package io.github.ieswar23.buddyup.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ieswar23.buddyup.data.local.BuddyUpDatabase
import io.github.ieswar23.buddyup.data.local.dao.ConnectionDao
import io.github.ieswar23.buddyup.data.local.dao.MeetupDao
import io.github.ieswar23.buddyup.data.local.dao.MessageDao
import io.github.ieswar23.buddyup.data.local.dao.PersonDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BuddyUpDatabase =
        Room.databaseBuilder(context, BuddyUpDatabase::class.java, BuddyUpDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePersonDao(db: BuddyUpDatabase): PersonDao = db.personDao()

    @Provides
    fun provideConnectionDao(db: BuddyUpDatabase): ConnectionDao = db.connectionDao()

    @Provides
    fun provideMessageDao(db: BuddyUpDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideMeetupDao(db: BuddyUpDatabase): MeetupDao = db.meetupDao()
}
