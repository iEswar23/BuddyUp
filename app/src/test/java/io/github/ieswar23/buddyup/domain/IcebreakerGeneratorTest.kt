package io.github.ieswar23.buddyup.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.buddyup.domain.icebreakers.Icebreaker.Source
import io.github.ieswar23.buddyup.domain.icebreakers.IcebreakerGenerator
import io.github.ieswar23.buddyup.domain.model.Interests
import io.github.ieswar23.buddyup.domain.model.Meetup
import io.github.ieswar23.buddyup.domain.model.MeetupCategory
import io.github.ieswar23.buddyup.domain.model.Person
import io.github.ieswar23.buddyup.fakes.meetup
import io.github.ieswar23.buddyup.fakes.person
import io.github.ieswar23.buddyup.fakes.testProfile
import org.junit.Test

class IcebreakerGeneratorTest {

    private val generator = IcebreakerGenerator()

    // testProfile: Bengaluru; Hiking, Coffee, Chess, Photography.

    @Test
    fun `shared interests come first, in the order the other person listed them`() {
        val them = person("p1", interests = listOf("Yoga", "Photography", "Hiking"))

        val result = generator.generate(testProfile, them)

        assertThat(result).hasSize(3)
        assertThat(result.take(2).map { it.source }).containsExactly(Source.SHARED_INTEREST, Source.SHARED_INTEREST)
        assertThat(result.take(2).map { it.interest }).containsExactly("Photography", "Hiking").inOrder()
        // With only two shared interests, the third slot is filled from their profile.
        assertThat(result[2].source).isEqualTo(Source.PROFILE)
    }

    @Test
    fun `never more than three suggestions, even with many shared interests`() {
        val them = person("p1", interests = listOf("Hiking", "Coffee", "Chess", "Photography"))

        val result = generator.generate(testProfile, them)

        assertThat(result).hasSize(IcebreakerGenerator.MAX_SUGGESTIONS)
        assertThat(result.map { it.source }.toSet()).containsExactly(Source.SHARED_INTEREST)
        assertThat(result.map { it.interest }).containsExactly("Hiking", "Coffee", "Chess").inOrder()
    }

    @Test
    fun `shared interests are matched case-insensitively`() {
        val them = person("p1", interests = listOf("hiking"))

        val result = generator.generate(testProfile, them)

        assertThat(result.first().source).isEqualTo(Source.SHARED_INTEREST)
        assertThat(result.first().interest).isEqualTo("hiking")
    }

    @Test
    fun `falls back to profile, their interests and generic starters when nothing is shared`() {
        val them = person("p1", interests = listOf("Cricket", "Yoga"), city = "Bengaluru", neighborhood = "Jayanagar")

        val result = generator.generate(testProfile, them)

        assertThat(result.map { it.source })
            .containsExactly(Source.PROFILE, Source.THEIR_INTEREST, Source.GENERIC).inOrder()
        assertThat(result[0].text).contains("Jayanagar")
        assertThat(result[1].interest).isEqualTo("Cricket")
        assertThat(result[1].text).contains("cricket")
    }

    @Test
    fun `someone in another city gets a city starter instead of a neighborhood one`() {
        val them = person("p1", interests = emptyList(), city = "Chicago", neighborhood = "Wicker Park")

        val profileStarter = generator.generate(testProfile, them).first { it.source == Source.PROFILE }

        assertThat(profileStarter.text).contains("Chicago")
        assertThat(profileStarter.text).doesNotContain("Wicker Park")
    }

    @Test
    fun `a bio about moving gets a starter about the move`() {
        val them = person("p1", interests = emptyList(), bio = "Moved from Mysuru last year. Weekend sketcher.")

        val profileStarter = generator.generate(testProfile, them).first { it.source == Source.PROFILE }

        assertThat(profileStarter.text).containsMatch("(?i)move")
        assertThat(profileStarter.text).contains("Bengaluru")
    }

    @Test
    fun `with no interests or location there are still three generic starters`() {
        val them = person("p1", interests = emptyList(), city = "", neighborhood = "", bio = "")

        val result = generator.generate(testProfile, them)

        assertThat(result).hasSize(3)
        assertThat(result.map { it.source }.toSet()).containsExactly(Source.GENERIC)
    }

    @Test
    fun `output is deterministic for the same people`() {
        val them = person("p42", interests = listOf("Reading", "Coffee", "Dogs"))

        val first = generator.generate(testProfile, them)
        val second = IcebreakerGenerator().generate(testProfile, them)

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun `no duplicates, no unfilled placeholders and at most three for every catalog interest`() {
        val catalog = Interests.all.map { it.name }
        catalog.forEachIndexed { index, interest ->
            listOf("", "Austin").forEach { city ->
                val them = person(
                    id = "p$index$city",
                    interests = listOf(interest, catalog[(index + 7) % catalog.size]),
                    city = city,
                    neighborhood = if (city.isEmpty()) "" else "Zilker",
                )
                val me = testProfile.copy(interests = listOf(interest, "Coffee"))

                val result = generator.generate(me, them)

                assertThat(result.size).isAtMost(3)
                assertThat(result).isNotEmpty()
                assertThat(result.map { it.text.lowercase() }).containsNoDuplicates()
                result.forEach { assertThat(it.text).doesNotContain("{") }
                assertThat(result.first().source).isEqualTo(Source.SHARED_INTEREST)
            }
        }
    }

    @Test
    fun `respects a custom maximum`() {
        val them = person("p1", interests = listOf("Hiking", "Coffee"))

        assertThat(generator.generate(testProfile, them, max = 1)).hasSize(1)
        assertThat(generator.generate(testProfile, them, max = 0)).isEmpty()
    }

    @Test
    fun `unknown interests still get a sensible starter`() {
        val them = person("p1", interests = listOf("Origami"))
        val me = testProfile.copy(interests = listOf("Origami"))

        val result = generator.generate(me, them)

        assertThat(result.first().source).isEqualTo(Source.SHARED_INTEREST)
        assertThat(result.first().text).contains("origami")
    }

    @Test
    fun `every catalog interest has its own curated templates`() {
        Interests.all.forEach { interest ->
            val variants = IcebreakerGenerator.INTEREST_TEMPLATES[interest.name.lowercase()]
            assertThat(variants).isNotNull()
            assertThat(variants!!.size).isAtLeast(3)
            assertThat(variants.count { "{" !in it }).isAtLeast(2)
            assertThat(variants).containsNoDuplicates()
        }
    }

    @Test
    fun `each set covers different topics before repeating one`() {
        val them = person("p1", interests = listOf("Photography", "Hiking"))

        val result = generator.generate(testProfile, them)

        val sharedTopics = result.filter { it.source == Source.SHARED_INTEREST }.map { it.interest }
        assertThat(sharedTopics).containsNoDuplicates()
    }

    @Test
    fun `refreshing moves to the next set, never repeats within a cycle and wraps around`() {
        val them = person("p7", interests = listOf("Reading", "Coffee", "Dogs", "Travel"))
        val setCount = generator.setCount(testProfile, them)
        assertThat(setCount).isGreaterThan(2)

        val sets = (0 until setCount).map { generator.generate(testProfile, them, round = it) }

        sets.forEach { set ->
            assertThat(set).hasSize(3)
            assertThat(set.map { it.text.lowercase() }).containsNoDuplicates()
        }
        assertThat(sets.toSet()).hasSize(setCount)
        assertThat(sets[1]).containsNoneIn(sets[0])
        assertThat(generator.generate(testProfile, them, round = setCount)).isEqualTo(sets[0])
        assertThat(generator.generate(testProfile, them, round = -1)).isEqualTo(sets.last())
    }

    @Test
    fun `shared interests stay first in later sets too`() {
        // Four shared interests with three variants each fill the first four sets.
        val them = person("p1", interests = listOf("Hiking", "Coffee", "Chess", "Photography", "Yoga"))

        val laterSets = (0 until 4).map { generator.generate(testProfile, them, round = it) }

        laterSets.flatten().forEach { assertThat(it.source).isEqualTo(Source.SHARED_INTEREST) }
        assertThat(generator.generate(testProfile, them, round = 4).map { it.source })
            .doesNotContain(Source.SHARED_INTEREST)
    }

    @Test
    fun `the number of sets depends on the set size`() {
        val them = person("p1", interests = emptyList(), city = "", neighborhood = "", bio = "")

        assertThat(generator.setCount(testProfile, them)).isEqualTo(2)
        assertThat(generator.setCount(testProfile, them, max = 6)).isEqualTo(1)
        assertThat(generator.generate(testProfile, them, round = 5, max = 6))
            .isEqualTo(generator.generate(testProfile, them, max = 6))
        assertThat(generator.setCount(testProfile, them, max = 0)).isEqualTo(0)
    }

    @Test
    fun `a meetup matching a shared interest follows the shared-interest starters`() {
        val them = person("p1", interests = listOf("Photography", "Hiking"))
        val trek = meetup("e03", "Sunrise Trek to Skandagiri", MeetupCategory.OUTDOORS)

        val result = generator.generate(testProfile, them, meetups = listOf(trek))

        assertThat(result.map { it.source })
            .containsExactly(Source.SHARED_INTEREST, Source.SHARED_INTEREST, Source.MEETUP).inOrder()
        assertThat(result[2].text).contains("Sunrise Trek to Skandagiri")
        assertThat(result[2].interest).isEqualTo("Hiking")
    }

    @Test
    fun `shared-interest meetups beat ones that only match their interests, then the soonest wins`() {
        val them = person("p1", interests = listOf("Yoga", "Hiking"))
        val yoga = meetup("e10", "Rooftop Yoga", MeetupCategory.FITNESS, startsInMinutes = 60)
        val laterTrek = meetup("e11", "Nandi Hills Trek", MeetupCategory.OUTDOORS, startsInMinutes = 3 * 24 * 60)
        val soonerTrek = meetup("e12", "Turahalli Forest Walk", MeetupCategory.OUTDOORS, startsInMinutes = 24 * 60)

        val invites = allSets(them, meetups = listOf(yoga, laterTrek, soonerTrek))
            .filter { it.source == Source.MEETUP }

        assertThat(invites).hasSize(2)
        assertThat(invites[0].text).contains("Turahalli Forest Walk")
        assertThat(invites[1].text).contains("Nandi Hills Trek")
    }

    @Test
    fun `meetups elsewhere, full or unrelated to either person are never suggested`() {
        val them = person("p1", interests = listOf("Hiking", "Coffee"))
        val meetups = listOf(
            meetup("e1", "Lakefront Trail Run", MeetupCategory.OUTDOORS, city = "Chicago"),
            meetup("e2", "Packed Coffee Crawl", MeetupCategory.COFFEE, attendeeCount = 20, capacity = 20),
            meetup("e3", "Indie Night", MeetupCategory.MUSIC),
        )

        assertThat(allSets(them, meetups).map { it.source }).doesNotContain(Source.MEETUP)
    }

    @Test
    fun `meetups are only suggested when both people live in that city`() {
        val them = person("p1", interests = listOf("Hiking"), city = "Chicago", neighborhood = "Wicker Park")
        val chicagoHike = meetup("e1", "Lakefront Trail Hike", MeetupCategory.OUTDOORS, city = "Chicago")

        assertThat(allSets(them, listOf(chicagoHike)).map { it.source }).doesNotContain(Source.MEETUP)
    }

    @Test
    fun `a meetup the user already joined becomes an invitation to come along`() {
        val them = person("p1", interests = listOf("Coffee"))
        val walk = meetup("e01", "Sunrise Coffee Walk", MeetupCategory.COFFEE, isJoined = true, attendeeCount = 25, capacity = 25)

        val invite = allSets(them, listOf(walk)).single { it.source == Source.MEETUP }

        assertThat(invite.text).contains("Sunrise Coffee Walk")
        assertThat(invite.text).containsMatch("I'm going|I'll be there")
    }

    @Test
    fun `every interest that maps to a meetup category is in the catalog`() {
        val catalog = Interests.all.map { it.name.lowercase() }
        assertThat(catalog).containsAtLeastElementsIn(IcebreakerGenerator.INTEREST_CATEGORIES.keys)
    }

    /** Every distinct starter across a full refresh cycle, in the order they are first shown. */
    private fun allSets(them: Person, meetups: List<Meetup>) =
        (0 until generator.setCount(testProfile, them, meetups)).flatMap { round ->
            generator.generate(testProfile, them, meetups, round)
        }.distinct()
}
