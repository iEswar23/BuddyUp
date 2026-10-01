package io.github.ieswar23.buddyup.domain.model

data class ProfileValidation(
    val nameError: String? = null,
    val bioError: String? = null,
    val interestsError: String? = null,
) {
    val isValid: Boolean get() = nameError == null && bioError == null && interestsError == null
}

object ProfileValidator {

    fun validate(profile: UserProfile): ProfileValidation {
        val name = profile.name.trim()
        val nameError = when {
            name.isEmpty() -> "Tell people what to call you"
            name.length < 2 -> "That's a bit short"
            name.length > UserProfile.MAX_NAME_LENGTH -> "Keep it under ${UserProfile.MAX_NAME_LENGTH} characters"
            else -> null
        }
        val bioError = if (profile.bio.length > UserProfile.MAX_BIO_LENGTH) {
            "Keep your bio under ${UserProfile.MAX_BIO_LENGTH} characters"
        } else {
            null
        }
        val count = profile.interests.size
        val interestsError = when {
            count < UserProfile.MIN_INTERESTS -> "Pick at least ${UserProfile.MIN_INTERESTS} interests"
            count > UserProfile.MAX_INTERESTS -> "Pick at most ${UserProfile.MAX_INTERESTS} interests"
            else -> null
        }
        return ProfileValidation(nameError, bioError, interestsError)
    }
}
