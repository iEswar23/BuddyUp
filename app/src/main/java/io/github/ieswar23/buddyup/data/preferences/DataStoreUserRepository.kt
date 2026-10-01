package io.github.ieswar23.buddyup.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.ieswar23.buddyup.data.repository.UserRepository
import io.github.ieswar23.buddyup.domain.model.AgeRange
import io.github.ieswar23.buddyup.domain.model.AppSettings
import io.github.ieswar23.buddyup.domain.model.ThemeMode
import io.github.ieswar23.buddyup.domain.model.UserProfile
import io.github.ieswar23.buddyup.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreUserRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val timeProvider: TimeProvider,
) : UserRepository {

    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    override val profile: Flow<UserProfile> = preferences.map { prefs ->
        UserProfile(
            name = prefs[Keys.NAME].orEmpty(),
            ageRange = AgeRange.fromKey(prefs[Keys.AGE_RANGE]),
            city = prefs[Keys.CITY] ?: UserProfile.EMPTY.city,
            bio = prefs[Keys.BIO].orEmpty(),
            interests = prefs[Keys.INTERESTS]?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty(),
        )
    }.distinctUntilChanged()

    override val settings: Flow<AppSettings> = preferences.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME]?.let { key -> ThemeMode.entries.firstOrNull { it.name == key } }
                ?: ThemeMode.SYSTEM,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
        )
    }.distinctUntilChanged()

    override val onboardingComplete: Flow<Boolean> =
        preferences.map { it[Keys.ONBOARDING_COMPLETE] ?: false }.distinctUntilChanged()

    override val memberSince: Flow<Long?> = preferences.map { it[Keys.MEMBER_SINCE] }.distinctUntilChanged()

    override suspend fun saveProfile(profile: UserProfile) {
        dataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name.trim()
            prefs[Keys.AGE_RANGE] = profile.ageRange.name
            prefs[Keys.CITY] = profile.city
            prefs[Keys.BIO] = profile.bio.trim()
            prefs[Keys.INTERESTS] = profile.interests.joinToString(SEPARATOR)
            if (prefs[Keys.MEMBER_SINCE] == null) prefs[Keys.MEMBER_SINCE] = timeProvider.now()
        }
    }

    override suspend fun completeOnboarding() {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = true }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val NAME = stringPreferencesKey("profile_name")
        val AGE_RANGE = stringPreferencesKey("profile_age_range")
        val CITY = stringPreferencesKey("profile_city")
        val BIO = stringPreferencesKey("profile_bio")
        val INTERESTS = stringPreferencesKey("profile_interests")
        val MEMBER_SINCE = longPreferencesKey("member_since")
        val THEME = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }

    private companion object {
        const val SEPARATOR = "|"
    }
}
