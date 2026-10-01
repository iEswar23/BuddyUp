package io.github.ieswar23.buddyup.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.buddyup.domain.scoring.MatchScorer
import io.github.ieswar23.buddyup.fakes.NOW
import io.github.ieswar23.buddyup.fakes.person
import org.junit.Test

class MatchScorerTest {

    private val scorer = MatchScorer()

    @Test
    fun `identical interests, zero distance and active now scores 100`() {
        val other = person("p1", interests = listOf("Hiking", "Chess"), distanceKm = 0.0, lastActiveMinutesAgo = 0)
        val result = scorer.score(listOf("Hiking", "Chess"), other, NOW)
        assertThat(result.percent).isEqualTo(100)
        assertThat(result.sharedInterests).containsExactly("Hiking", "Chess").inOrder()
    }

    @Test
    fun `no shared interests, far away and inactive scores very low`() {
        val other = person("p1", interests = listOf("Cricket"), distanceKm = 900.0, lastActiveMinutesAgo = 60L * 24 * 30)
        val result = scorer.score(listOf("Chess", "Reading"), other, NOW)
        assertThat(result.interestScore).isEqualTo(0.0)
        assertThat(result.distanceScore).isEqualTo(0.0)
        assertThat(result.percent).isEqualTo(2) // only the 0.1 inactive floor * 15%
        assertThat(result.sharedInterests).isEmpty()
    }

    @Test
    fun `jaccard similarity is intersection over union and case insensitive`() {
        val similarity = MatchScorer.jaccard(listOf("hiking", "Chess", "Coffee"), listOf("Hiking", "coffee", "Yoga", "Reading"))
        // shared = {hiking, coffee} = 2, union = {hiking, chess, coffee, yoga, reading} = 5
        assertThat(similarity).isWithin(1e-9).of(0.4)
    }

    @Test
    fun `jaccard of two empty sets is zero rather than NaN`() {
        assertThat(MatchScorer.jaccard(emptyList(), emptyList())).isEqualTo(0.0)
    }

    @Test
    fun `more shared interests always means a higher score`() {
        val mine = listOf("Hiking", "Coffee", "Chess", "Photography")
        val oneShared = person("a", interests = listOf("Hiking", "Cricket", "Movies", "Tech"))
        val threeShared = person("b", interests = listOf("Hiking", "Coffee", "Chess", "Tech"))
        assertThat(scorer.score(mine, threeShared, NOW).percent)
            .isGreaterThan(scorer.score(mine, oneShared, NOW).percent)
    }

    @Test
    fun `closer people score higher when everything else is equal`() {
        val mine = listOf("Hiking", "Coffee")
        val near = person("near", distanceKm = 2.0)
        val far = person("far", distanceKm = 40.0)
        assertThat(scorer.score(mine, near, NOW).percent).isGreaterThan(scorer.score(mine, far, NOW).percent)
    }

    @Test
    fun `distance score falls off linearly and clamps beyond max distance`() {
        assertThat(MatchScorer.distanceScore(0.0)).isEqualTo(1.0)
        assertThat(MatchScorer.distanceScore(25.0)).isWithin(1e-9).of(0.5)
        assertThat(MatchScorer.distanceScore(500.0)).isEqualTo(0.0)
        assertThat(MatchScorer.distanceScore(-3.0)).isEqualTo(1.0)
    }

    @Test
    fun `activity weighting rewards recently active people`() {
        assertThat(MatchScorer.activityScore(5)).isEqualTo(1.0)
        assertThat(MatchScorer.activityScore(120)).isEqualTo(0.8)
        assertThat(MatchScorer.activityScore(600)).isEqualTo(0.6)
        assertThat(MatchScorer.activityScore(60L * 24 * 3)).isEqualTo(0.3)
        assertThat(MatchScorer.activityScore(60L * 24 * 30)).isEqualTo(0.1)
    }

    @Test
    fun `score is always within 0 to 100`() {
        val extremes = listOf(
            person("x", interests = emptyList(), distanceKm = 10_000.0, lastActiveMinutesAgo = 100_000),
            person("y", interests = listOf("Hiking", "Coffee", "Chess", "Photography"), distanceKm = 0.0, lastActiveMinutesAgo = 0),
        )
        extremes.forEach {
            val percent = scorer.score(listOf("Hiking", "Coffee", "Chess", "Photography"), it, NOW).percent
            assertThat(percent).isAtLeast(0)
            assertThat(percent).isAtMost(100)
        }
    }
}
