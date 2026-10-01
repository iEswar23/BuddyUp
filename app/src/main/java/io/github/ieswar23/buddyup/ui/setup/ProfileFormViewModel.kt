package io.github.ieswar23.buddyup.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ieswar23.buddyup.data.repository.UserRepository
import io.github.ieswar23.buddyup.domain.model.AgeRange
import io.github.ieswar23.buddyup.domain.model.ProfileValidation
import io.github.ieswar23.buddyup.domain.model.ProfileValidator
import io.github.ieswar23.buddyup.domain.model.UserProfile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileFormState(
    val profile: UserProfile = UserProfile.EMPTY,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val showErrors: Boolean = false,
) {
    val validation: ProfileValidation get() = ProfileValidator.validate(profile)
    val canAddMoreInterests: Boolean get() = profile.interests.size < UserProfile.MAX_INTERESTS
}

@HiltViewModel
class ProfileFormViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileFormState())
    val state: StateFlow<ProfileFormState> = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    init {
        viewModelScope.launch {
            val existing = userRepository.profile.first()
            _state.update { it.copy(profile = existing, isLoaded = true) }
        }
    }

    fun onNameChange(name: String) = updateProfile { copy(name = name.take(UserProfile.MAX_NAME_LENGTH)) }

    fun onAgeRangeChange(range: AgeRange) = updateProfile { copy(ageRange = range) }

    fun onCityChange(city: String) = updateProfile { copy(city = city) }

    fun onBioChange(bio: String) = updateProfile { copy(bio = bio.take(UserProfile.MAX_BIO_LENGTH)) }

    fun onInterestToggle(interest: String) = updateProfile {
        when {
            interest in interests -> copy(interests = interests - interest)
            interests.size >= UserProfile.MAX_INTERESTS -> this
            else -> copy(interests = interests + interest)
        }
    }

    fun save() {
        val current = _state.value
        if (!current.validation.isValid) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        if (current.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            userRepository.saveProfile(current.profile)
            userRepository.completeOnboarding()
            _state.update { it.copy(isSaving = false) }
            _saved.send(Unit)
        }
    }

    private inline fun updateProfile(transform: UserProfile.() -> UserProfile) {
        _state.update { it.copy(profile = it.profile.transform()) }
    }
}
