package io.github.ieswar23.buddyup.domain.icebreakers

import io.github.ieswar23.buddyup.domain.model.Meetup
import io.github.ieswar23.buddyup.domain.model.MeetupCategory
import io.github.ieswar23.buddyup.domain.model.Person
import io.github.ieswar23.buddyup.domain.model.UserProfile
import io.github.ieswar23.buddyup.domain.scoring.MatchScorer
import javax.inject.Inject

/** A conversation starter, plus where it came from (used for the chip row's caption and tests). */
data class Icebreaker(
    val text: String,
    val source: Source,
    val interest: String? = null,
) {
    enum class Source { SHARED_INTEREST, MEETUP, PROFILE, THEIR_INTEREST, GENERIC }
}

/**
 * Builds sets of [MAX_SUGGESTIONS] conversation starters for a chat between the user and [Person].
 *
 * Suggestions come from curated templates (no network or AI calls) and are ranked like this:
 *  1. starters about **shared interests**, in the order the other person listed them — one per
 *     interest;
 *  2. invitations to upcoming **meetups** in the city you both live in that match a shared interest
 *     (or, failing that, one of theirs);
 *  3. the remaining variants of the shared-interest starters;
 *  4. a mix of **profile** starters (their bio, neighborhood or city), starters about interests of
 *     **theirs** the user doesn't share, and **general** starters, so there is always something.
 *
 * That ranked list is split into sets: set 0 is the most personal, and each refresh moves on to
 * the next set, wrapping around after [setCount]. Template variants are rotated by a stable hash of
 * the other person's id, so a given pair always sees the same sets in the same order while
 * different people get some variety. A set never contains duplicates.
 *
 * The class is deliberately free of Android dependencies so it can be unit tested in isolation.
 */
class IcebreakerGenerator @Inject constructor() {

    /**
     * Set number [round] (any integer; it wraps around) of up to [max] starters. [meetups] are the
     * upcoming meetups the user can see; only ones in the city both people live in are suggested.
     */
    fun generate(
        me: UserProfile,
        them: Person,
        meetups: List<Meetup> = emptyList(),
        round: Int = 0,
        max: Int = MAX_SUGGESTIONS,
    ): List<Icebreaker> {
        if (max <= 0) return emptyList()
        val sets = sets(rankedStarters(me, them, meetups), max)
        return if (sets.isEmpty()) emptyList() else sets[Math.floorMod(round, sets.size)]
    }

    /** How many distinct sets [generate] cycles through before repeating. */
    fun setCount(me: UserProfile, them: Person, meetups: List<Meetup> = emptyList(), max: Int = MAX_SUGGESTIONS): Int =
        if (max <= 0) 0 else sets(rankedStarters(me, them, meetups), max).size

    /**
     * Splits [ranked] into sets of [max], keeping rank order but preferring different topics within
     * a set (so one set doesn't ask two things about the same interest). A short last set is topped
     * up with the highest-ranked starters it doesn't already contain.
     */
    private fun sets(ranked: List<Icebreaker>, max: Int): List<List<Icebreaker>> {
        if (ranked.size <= max) return if (ranked.isEmpty()) emptyList() else listOf(ranked)
        val remaining = ranked.toMutableList()
        val sets = mutableListOf<List<Icebreaker>>()
        while (remaining.isNotEmpty()) {
            val set = mutableListOf<Icebreaker>()
            val topics = HashSet<String>()
            for (candidate in remaining) {
                if (set.size == max) break
                if (topics.add(candidate.topic)) set += candidate
            }
            for (candidate in remaining) {
                if (set.size == max) break
                if (candidate !in set) set += candidate
            }
            remaining.removeAll(set)
            sets += set
        }
        val last = sets.last()
        if (last.size < max) sets[sets.lastIndex] = last + ranked.filterNot { it in last }.take(max - last.size)
        return sets
    }

    private val Icebreaker.topic: String
        get() = when (source) {
            // At most one profile question and one meetup invite per set.
            Icebreaker.Source.PROFILE, Icebreaker.Source.MEETUP -> source.name
            Icebreaker.Source.GENERIC -> text
            Icebreaker.Source.SHARED_INTEREST, Icebreaker.Source.THEIR_INTEREST -> interest.orEmpty().lowercase()
        }

    private fun rankedStarters(me: UserProfile, them: Person, meetups: List<Meetup>): List<Icebreaker> {
        // Small, non-negative and stable across runs (String.hashCode is specified by the JLS).
        val seed = Math.floorMod(them.id.hashCode(), SEED_RANGE)
        val placeholders = mapOf(
            "{name}" to them.firstName,
            "{city}" to them.city,
            "{neighborhood}" to them.neighborhood,
        )
        val shared = MatchScorer.sharedInterests(me.interests, them.interests)
        val sharedKeys = shared.mapTo(HashSet()) { it.lowercase() }

        val sharedByInterest = shared.mapIndexed { index, interest ->
            rotated(templatesFor(interest), seed + index).mapNotNull { template ->
                fill(template, placeholders)?.let { Icebreaker(it, Icebreaker.Source.SHARED_INTEREST, interest) }
            }
        }
        val sharedStarters = sharedByInterest.mapNotNull { it.firstOrNull() } +
            meetupStarters(me, them, shared, meetups, seed) +
            interleave(sharedByInterest.map { it.drop(1) })
        val profileStarters = rotated(profileTemplates(me, them), seed).mapNotNull { template ->
            fill(template, placeholders)?.let { Icebreaker(it, Icebreaker.Source.PROFILE) }
        }
        val theirStarters = them.interests
            .filter { it.isNotBlank() && it.lowercase() !in sharedKeys }
            .distinctBy { it.lowercase() }
            .mapIndexed { index, interest ->
                val template = CURIOUS_TEMPLATES[(seed + index) % CURIOUS_TEMPLATES.size]
                Icebreaker(template.replace("{interest}", interest.lowercase()), Icebreaker.Source.THEIR_INTEREST, interest)
            }
        val generalStarters = rotated(GENERAL_TEMPLATES, seed).map { Icebreaker(it, Icebreaker.Source.GENERIC) }

        val seen = HashSet<String>()
        return (sharedStarters + interleave(listOf(profileStarters, theirStarters, generalStarters)))
            .filter { seen.add(it.text.lowercase()) }
    }

    /**
     * Invitations to meetups in the city both people live in whose category matches a shared
     * interest (ranked first) or one of theirs, soonest first. Full meetups are skipped.
     */
    private fun meetupStarters(
        me: UserProfile,
        them: Person,
        shared: List<String>,
        meetups: List<Meetup>,
        seed: Int,
    ): List<Icebreaker> {
        if (them.city.isBlank() || !me.city.equals(them.city, ignoreCase = true)) return emptyList()
        return meetups
            .filter { it.city.equals(them.city, ignoreCase = true) && !it.isFull }
            .mapNotNull { meetup ->
                val sharedMatch = shared.firstOrNull { categoryOf(it) == meetup.category }
                val match = sharedMatch ?: them.interests.firstOrNull { categoryOf(it) == meetup.category }
                match?.let { MeetupMatch(meetup, it, isShared = sharedMatch != null) }
            }
            .sortedWith(compareBy<MeetupMatch> { !it.isShared }.thenBy { it.meetup.startsAt }.thenBy { it.meetup.id })
            .take(MAX_MEETUP_STARTERS)
            .mapIndexed { index, (meetup, interest) ->
                val templates = if (meetup.isJoined) JOINED_MEETUP_TEMPLATES else MEETUP_TEMPLATES
                val text = templates[(seed + index) % templates.size]
                    .replace("{meetup}", meetup.title)
                    .replace("{emoji}", meetup.emoji)
                Icebreaker(text, Icebreaker.Source.MEETUP, interest)
            }
    }

    private data class MeetupMatch(val meetup: Meetup, val interest: String, val isShared: Boolean)

    private fun categoryOf(interest: String): MeetupCategory? = INTEREST_CATEGORIES[interest.lowercase()]

    private fun profileTemplates(me: UserProfile, them: Person): List<String> {
        val bio = them.bio.lowercase()
        val sameCity = me.city.isNotBlank() && me.city.equals(them.city, ignoreCase = true)
        return when {
            them.city.isNotBlank() && MOVED_KEYWORDS.any { it in bio } -> MOVED_TEMPLATES
            sameCity && them.neighborhood.isNotBlank() -> NEIGHBORHOOD_TEMPLATES
            !sameCity && them.city.isNotBlank() -> OTHER_CITY_TEMPLATES
            else -> emptyList()
        }
    }

    /** [list] starting at index [start] (mod size) and wrapping around. */
    private fun <T> rotated(list: List<T>, start: Int): List<T> =
        if (list.isEmpty()) list else List(list.size) { list[(start + it) % list.size] }

    /** Round-robin merge: first item of each list, then the second of each, and so on. */
    private fun <T> interleave(lists: List<List<T>>): List<T> {
        val depth = lists.maxOfOrNull { it.size } ?: 0
        return (0 until depth).flatMap { index -> lists.mapNotNull { it.getOrNull(index) } }
    }

    /** Fills the placeholders in [template], or returns null if one of them has no value. */
    private fun fill(template: String, placeholders: Map<String, String>): String? {
        var result = template
        for ((key, value) in placeholders) {
            if (key !in result) continue
            if (value.isBlank()) return null
            result = result.replace(key, value.trim())
        }
        return result
    }

    private fun templatesFor(interest: String): List<String> =
        INTEREST_TEMPLATES[interest.lowercase()]
            ?: CUSTOM_INTEREST_TEMPLATES.map { it.replace("{interest}", interest.lowercase()) }

    companion object {
        const val MAX_SUGGESTIONS = 3
        private const val SEED_RANGE = 1_000
        private const val MAX_MEETUP_STARTERS = 2

        /** Which kind of meetup each catalog interest relates to (interests without one are left out). */
        internal val INTEREST_CATEGORIES: Map<String, MeetupCategory> = mapOf(
            "coffee" to MeetupCategory.COFFEE,
            "hiking" to MeetupCategory.OUTDOORS,
            "cycling" to MeetupCategory.OUTDOORS,
            "board games" to MeetupCategory.GAMES,
            "chess" to MeetupCategory.GAMES,
            "gaming" to MeetupCategory.GAMES,
            "indie music" to MeetupCategory.MUSIC,
            "dancing" to MeetupCategory.MUSIC,
            "cooking" to MeetupCategory.FOOD,
            "baking" to MeetupCategory.FOOD,
            "running" to MeetupCategory.FITNESS,
            "yoga" to MeetupCategory.FITNESS,
            "cricket" to MeetupCategory.FITNESS,
            "football" to MeetupCategory.FITNESS,
            "basketball" to MeetupCategory.FITNESS,
            "tennis" to MeetupCategory.FITNESS,
            "badminton" to MeetupCategory.FITNESS,
            "photography" to MeetupCategory.CREATIVE,
            "painting" to MeetupCategory.CREATIVE,
            "writing" to MeetupCategory.CREATIVE,
            "startups" to MeetupCategory.NETWORKING,
            "tech" to MeetupCategory.NETWORKING,
        )

        private val MEETUP_TEMPLATES = listOf(
            "Want to check out {meetup} together? {emoji}",
            "Have you seen {meetup}? Could be fun to go together {emoji}",
        )
        private val JOINED_MEETUP_TEMPLATES = listOf(
            "I'm going to {meetup} — want to come along? {emoji}",
            "Joining {meetup}? I'll be there! {emoji}",
        )

        private val MOVED_KEYWORDS = listOf("moved", "new to", "new in", "relocated", "just arrived")

        /**
         * Three curated variants for every interest in the
         * [io.github.ieswar23.buddyup.domain.model.Interests] catalog. Each interest has at least two
         * variants without placeholders, so a set can always be filled.
         */
        internal val INTEREST_TEMPLATES: Map<String, List<String>> = mapOf(
            "hiking" to listOf(
                "What's the best trail you've done near {city}? 🥾",
                "Up for a sunrise hike some weekend? 🥾",
                "Mountains or forest trails — what's your pick? 🥾",
            ),
            "coffee" to listOf(
                "Where's the best coffee near {neighborhood}? ☕",
                "Filter coffee or fancy pour-over? ☕",
                "Want to try a new café together this week? ☕",
            ),
            "board games" to listOf(
                "What's your go-to board game right now? 🎲",
                "Up for a board-game night sometime? 🎲",
                "Strategy games or quick party games? 🎲",
            ),
            "chess" to listOf(
                "Fancy a casual chess game sometime? ♟️",
                "What's your favourite chess opening? ♟️",
                "Blitz or long, slow games? ♟️",
            ),
            "photography" to listOf(
                "What do you love shooting most? 📷",
                "Photo walk around {neighborhood} sometime? 📷",
                "Phone camera or a proper camera? 📷",
            ),
            "cricket" to listOf(
                "Who are you backing this cricket season? 🏏",
                "Up for a weekend cricket match? 🏏",
                "Batter or bowler? 🏏",
            ),
            "cooking" to listOf(
                "What's your signature dish? 🍳",
                "Want to swap a favourite recipe? 🍳",
                "What's the best thing you've cooked lately? 🍳",
            ),
            "indie music" to listOf(
                "Which indie artist are you looping lately? 🎸",
                "Any good gigs coming up in {city}? 🎸",
                "What's an album everyone should hear? 🎸",
            ),
            "startups" to listOf(
                "Working on anything exciting lately? 🚀",
                "Any startup events in {city} worth going to? 🚀",
                "If you started a company tomorrow, what would it do? 🚀",
            ),
            "running" to listOf(
                "Do you have a favourite running route? 🏃",
                "Training for any races this year? 🏃",
                "Up for an easy morning run together? 🏃",
            ),
            "yoga" to listOf(
                "Any yoga studio you'd recommend? 🧘",
                "Morning or evening yoga person? 🧘",
                "What got you into yoga? 🧘",
            ),
            "reading" to listOf(
                "What are you reading right now? 📚",
                "What's a book you'd recommend to anyone? 📚",
                "Fiction or non-fiction person? 📚",
            ),
            "travel" to listOf(
                "Where's the last place you travelled to? ✈️",
                "What's next on your travel list? ✈️",
                "Best trip you've ever taken? ✈️",
            ),
            "gaming" to listOf(
                "What are you playing these days? 🎮",
                "Co-op or competitive games? 🎮",
                "What's your all-time favourite game? 🎮",
            ),
            "cycling" to listOf(
                "Any good cycling routes around {city}? 🚴",
                "Up for a weekend ride sometime? 🚴",
                "Road bike, MTB or city cruiser? 🚴",
            ),
            "painting" to listOf(
                "What are you painting lately? 🎨",
                "Watercolour, acrylic or something else? 🎨",
                "Want to do a sketch-and-coffee afternoon? 🎨",
            ),
            "football" to listOf(
                "Which football team do you support? ⚽",
                "Up for a weekend kickabout? ⚽",
                "Watching any big matches this week? ⚽",
            ),
            "basketball" to listOf(
                "Where do you usually shoot hoops? 🏀",
                "Pickup basketball game sometime? 🏀",
                "Who's your favourite team to watch? 🏀",
            ),
            "tennis" to listOf(
                "Where do you usually play tennis? 🎾",
                "Up for a rally some morning? 🎾",
                "Singles or doubles? 🎾",
            ),
            "badminton" to listOf(
                "Know a good badminton court near {neighborhood}? 🏸",
                "Badminton doubles sometime? 🏸",
                "How long have you been playing badminton? 🏸",
            ),
            "movies" to listOf(
                "Seen any good movies lately? 🎬",
                "What's your all-time comfort movie? 🎬",
                "Up for a movie night sometime? 🎬",
            ),
            "tech" to listOf(
                "What's the coolest thing you've built lately? 💻",
                "Any tech meetups in {city} worth checking out? 💻",
                "What's a gadget you couldn't live without? 💻",
            ),
            "volunteering" to listOf(
                "Where do you volunteer? I'd love to join sometime 🤝",
                "Which cause are you most into? 🤝",
                "Any volunteering events coming up? 🤝",
            ),
            "dogs" to listOf(
                "Do you have a dog? I need to see pictures 🐶",
                "Best dog-friendly spot in {city}? 🐶",
                "Big dogs or small dogs? 🐶",
            ),
            "baking" to listOf(
                "What's the last thing you baked? 🧁",
                "Sourdough or sweet bakes? 🧁",
                "Want to trade baking recipes? 🧁",
            ),
            "podcasts" to listOf(
                "Any podcast you'd recommend? 🎧",
                "What podcast are you bingeing lately? 🎧",
                "Interview shows or storytelling podcasts? 🎧",
            ),
            "writing" to listOf(
                "What do you like to write about? ✍️",
                "Working on any writing projects? ✍️",
                "Who's a writer that inspires you? ✍️",
            ),
            "gardening" to listOf(
                "What are you growing right now? 🌱",
                "Balcony garden or backyard? 🌱",
                "What's the easiest plant for a beginner? 🌱",
            ),
            "dancing" to listOf(
                "What style of dance are you into? 💃",
                "Know any good dance classes in {city}? 💃",
                "Up for a beginner dance class together? 💃",
            ),
            "stand-up comedy" to listOf(
                "Seen any good stand-up lately? 🎤",
                "Open mic night sometime? 🎤",
                "Who's your favourite comedian? 🎤",
            ),
        )

        /** For interests outside the catalog (e.g. from an older app version or the API). */
        private val CUSTOM_INTEREST_TEMPLATES = listOf(
            "What got you into {interest}? ✨",
            "We should do something {interest}-related soon! ✨",
            "What's your favourite thing about {interest}? ✨",
        )

        private val MOVED_TEMPLATES = listOf(
            "How are you finding {city} since the move?",
            "What do you miss most from before you moved to {city}?",
        )
        private val NEIGHBORHOOD_TEMPLATES = listOf(
            "Any hidden gems around {neighborhood}?",
            "How long have you lived in {neighborhood}?",
        )
        private val OTHER_CITY_TEMPLATES = listOf(
            "What's one thing I shouldn't miss in {city}?",
            "What's your favourite weekend spot in {city}?",
        )
        private val CURIOUS_TEMPLATES = listOf(
            "I've never tried {interest} — how did you get into it?",
            "What do you love most about {interest}?",
        )
        private val GENERAL_TEMPLATES = listOf(
            "What does a perfect weekend look like for you?",
            "Coffee sometime this week? ☕",
            "What's something you're looking forward to lately?",
            "What's the best thing that happened to you this week?",
            "Any hidden talents I should know about? 😄",
            "What's a small thing that always makes your day?",
        )
    }
}
