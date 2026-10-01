package io.github.ieswar23.buddyup.domain.scoring

import io.github.ieswar23.buddyup.domain.model.Person
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Computes a friendly 0–100 "compatibility" score between the current user and another person.
 *
 * The score blends three signals:
 *  - **Interests (60%)** — Jaccard similarity of the two interest sets. A square-root curve is
 *    applied so that a partial overlap (e.g. 2 of 8 interests) still reads as meaningful.
 *  - **Distance (25%)** — linear falloff from 1.0 at 0 km to 0.0 at [MAX_DISTANCE_KM].
 *  - **Activity (15%)** — recently active people are more likely to respond.
 *
 * The class is deliberately free of Android dependencies so it can be unit tested in isolation.
 */
class MatchScorer @Inject constructor() {

    fun score(myInterests: Collection<String>, person: Person, now: Long): MatchResult {
        val shared = sharedInterests(myInterests, person.interests)
        val interestScore = sqrt(jaccard(myInterests, person.interests))
        val distanceScore = distanceScore(person.distanceKm)
        val activityScore = activityScore(person.minutesSinceActive(now))
        val total = INTEREST_WEIGHT * interestScore +
            DISTANCE_WEIGHT * distanceScore +
            ACTIVITY_WEIGHT * activityScore
        return MatchResult(
            percent = (total * 100).roundToInt().coerceIn(0, 100),
            sharedInterests = shared,
            interestScore = interestScore,
            distanceScore = distanceScore,
            activityScore = activityScore,
        )
    }

    companion object {
        const val INTEREST_WEIGHT = 0.60
        const val DISTANCE_WEIGHT = 0.25
        const val ACTIVITY_WEIGHT = 0.15
        const val MAX_DISTANCE_KM = 50.0

        /** |A ∩ B| / |A ∪ B|, case-insensitive. Two empty sets have similarity 0. */
        fun jaccard(a: Collection<String>, b: Collection<String>): Double {
            val setA = a.map { it.lowercase() }.toSet()
            val setB = b.map { it.lowercase() }.toSet()
            val union = setA union setB
            if (union.isEmpty()) return 0.0
            return (setA intersect setB).size.toDouble() / union.size
        }

        /** Shared interests in the order the other person listed them, using their spelling. */
        fun sharedInterests(mine: Collection<String>, theirs: List<String>): List<String> {
            val myKeys = mine.map { it.lowercase() }.toSet()
            return theirs.filter { it.lowercase() in myKeys }.distinctBy { it.lowercase() }
        }

        fun distanceScore(distanceKm: Double): Double =
            (1.0 - distanceKm.coerceAtLeast(0.0) / MAX_DISTANCE_KM).coerceIn(0.0, 1.0)

        fun activityScore(minutesSinceActive: Long): Double = when {
            minutesSinceActive <= 15 -> 1.0
            minutesSinceActive <= 3 * 60 -> 0.8
            minutesSinceActive <= 24 * 60 -> 0.6
            minutesSinceActive <= 7 * 24 * 60 -> 0.3
            else -> 0.1
        }
    }
}

data class MatchResult(
    val percent: Int,
    val sharedInterests: List<String>,
    val interestScore: Double,
    val distanceScore: Double,
    val activityScore: Double,
) {
    val label: String
        get() = when {
            percent >= 80 -> "Great match"
            percent >= 60 -> "Good match"
            percent >= 40 -> "Some common ground"
            else -> "New perspective"
        }
}
