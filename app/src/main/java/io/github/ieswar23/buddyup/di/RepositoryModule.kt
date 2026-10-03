package io.github.ieswar23.buddyup.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ieswar23.buddyup.data.preferences.DataStoreUserRepository
import io.github.ieswar23.buddyup.data.repository.ChatRepository
import io.github.ieswar23.buddyup.data.repository.FriendsRepository
import io.github.ieswar23.buddyup.data.repository.MeetupRepository
import io.github.ieswar23.buddyup.data.repository.OfflineFirstChatRepository
import io.github.ieswar23.buddyup.data.repository.OfflineFirstFriendsRepository
import io.github.ieswar23.buddyup.data.repository.OfflineFirstMeetupRepository
import io.github.ieswar23.buddyup.data.repository.OfflineFirstPeopleRepository
import io.github.ieswar23.buddyup.data.repository.OfflineFirstRequestsRepository
import io.github.ieswar23.buddyup.data.repository.OfflineFirstSafetyRepository
import io.github.ieswar23.buddyup.data.repository.PeopleRepository
import io.github.ieswar23.buddyup.data.repository.RequestsRepository
import io.github.ieswar23.buddyup.data.repository.SafetyRepository
import io.github.ieswar23.buddyup.data.repository.UserRepository
import io.github.ieswar23.buddyup.util.SystemTimeProvider
import io.github.ieswar23.buddyup.util.TimeProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: DataStoreUserRepository): UserRepository

    @Binds
    @Singleton
    abstract fun bindPeopleRepository(impl: OfflineFirstPeopleRepository): PeopleRepository

    @Binds
    @Singleton
    abstract fun bindRequestsRepository(impl: OfflineFirstRequestsRepository): RequestsRepository

    @Binds
    @Singleton
    abstract fun bindFriendsRepository(impl: OfflineFirstFriendsRepository): FriendsRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: OfflineFirstChatRepository): ChatRepository

    @Binds
    @Singleton
    abstract fun bindMeetupRepository(impl: OfflineFirstMeetupRepository): MeetupRepository

    @Binds
    @Singleton
    abstract fun bindSafetyRepository(impl: OfflineFirstSafetyRepository): SafetyRepository
}
