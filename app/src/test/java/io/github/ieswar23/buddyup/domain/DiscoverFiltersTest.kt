package io.github.ieswar23.buddyup.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.buddyup.domain.model.DiscoverFilters
import io.github.ieswar23.buddyup.domain.model.ProfileValidator
import io.github.ieswar23.buddyup.fakes.person
import io.github.ieswar23.buddyup.fakes.testProfile
import org.junit.Test

class DiscoverFiltersTest {

    @Test
    fun `anywhere distance ignores the distance limit`() {
        val farAway = person("p", distanceKm = 1_500.0)
        assertThat(DiscoverFilters(maxDistanceKm = 50).matches(farAway)).isFalse()
        assertThat(DiscoverFilters(maxDistanceKm = DiscoverFilters.ANYWHERE_KM).matches(farAway)).isTrue()
    }

    @Test
    fun `interest filter requires at least one overlap and age must be in range`() {
        val filters = DiscoverFilters(minAge = 25, maxAge = 30, interests = setOf("Chess", "Yoga"))
        assertThat(filters.matches(person("a", age = 27, interests = listOf("Chess")))).isTrue()
        assertThat(filters.matches(person("b", age = 27, interests = listOf("Cricket")))).isFalse()
        assertThat(filters.matches(person("c", age = 35, interests = listOf("Chess")))).isFalse()
        assertThat(filters.activeCount).isEqualTo(2)
    }

    @Test
    fun `profile validator enforces name and 3 to 8 interests`() {
        assertThat(ProfileValidator.validate(testProfile).isValid).isTrue()
        val noName = ProfileValidator.validate(testProfile.copy(name = " "))
        assertThat(noName.nameError).isNotNull()
        val twoInterests = ProfileValidator.validate(testProfile.copy(interests = listOf("Chess", "Yoga")))
        assertThat(twoInterests.interestsError).isNotNull()
        val nineInterests = ProfileValidator.validate(testProfile.copy(interests = List(9) { "i$it" }))
        assertThat(nineInterests.isValid).isFalse()
    }
}
