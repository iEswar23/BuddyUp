package io.github.ieswar23.buddyup.data.simulation

import io.github.ieswar23.buddyup.domain.model.Person
import javax.inject.Inject
import kotlin.random.Random

/**
 * Produces short, friendly replies so conversations feel alive in an offline demo.
 * The reply is chosen by a light keyword classification of the user's last message.
 */
class ReplyGenerator @Inject constructor(private val random: Random) {

    enum class Intent { PLAN, QUESTION, THANKS, GREETING, GENERIC }

    fun classify(message: String): Intent {
        val text = message.lowercase()
        val words = text.split(Regex("[^a-z']+")).filter { it.isNotBlank() }.toSet()
        return when {
            PLAN_KEYWORDS.any { it in words } || text.contains("this week") -> Intent.PLAN
            text.trimEnd().endsWith("?") || text.contains("?") -> Intent.QUESTION
            THANKS_KEYWORDS.any { it in words } || text.contains("thank you") -> Intent.THANKS
            GREETING_KEYWORDS.any { it in words } -> Intent.GREETING
            else -> Intent.GENERIC
        }
    }

    fun replyTo(message: String, friend: Person, sharedInterests: List<String>): String {
        val intent = classify(message)
        val base = when (intent) {
            Intent.PLAN -> PLAN_REPLIES
            Intent.QUESTION -> QUESTION_REPLIES
            Intent.THANKS -> THANKS_REPLIES
            Intent.GREETING -> GREETING_REPLIES
            Intent.GENERIC -> GENERIC_REPLIES
        }.random(random)
        val interest = sharedInterests.randomOrNull(random) ?: friend.interests.randomOrNull(random)
        val shouldAddFollowUp = intent == Intent.GREETING || intent == Intent.GENERIC
        return if (interest != null && shouldAddFollowUp && random.nextInt(100) < FOLLOW_UP_CHANCE) {
            "$base ${INTEREST_FOLLOW_UPS.random(random).replace("{interest}", interest.lowercase())}"
        } else {
            base
        }
    }

    fun waveBackGreeting(friend: Person, sharedInterests: List<String>): String {
        val interest = sharedInterests.firstOrNull()
        return if (interest != null) {
            WAVE_BACK_WITH_INTEREST.random(random).replace("{interest}", interest.lowercase())
        } else {
            WAVE_BACK_GENERIC.random(random)
        }.replace("{neighborhood}", friend.neighborhood)
    }

    companion object {
        private const val FOLLOW_UP_CHANCE = 55

        private val PLAN_KEYWORDS = setOf(
            "coffee", "meet", "hang", "weekend", "saturday", "sunday", "tonight", "tomorrow",
            "join", "plan", "plans", "free", "lunch", "dinner", "brunch", "walk", "chai",
        )
        private val THANKS_KEYWORDS = setOf("thanks", "thx", "ty", "appreciate")
        private val GREETING_KEYWORDS = setOf("hi", "hey", "hello", "hiya", "yo", "namaste", "hola", "heyy", "hii")

        val PLAN_REPLIES = listOf(
            "That sounds fun! I'm free Saturday afternoon if that works?",
            "Yes please 🙌 Just tell me when and where.",
            "Count me in! Should we invite a couple more people?",
            "I'd love that. Sunday morning works best for me ☕",
            "Ooh, good idea. Let me check my shifts and get back to you tonight!",
        )
        val QUESTION_REPLIES = listOf(
            "Good question! Honestly, I'm still figuring that out myself 😄",
            "Haha yes, definitely. What about you?",
            "Hmm, I'd say mostly weekends — weekdays are chaos.",
            "Not yet, but it's been on my list forever!",
            "Pretty much! I can show you next time we meet.",
        )
        val THANKS_REPLIES = listOf(
            "Anytime! 😊",
            "Of course, happy to help!",
            "No worries at all 🙏",
        )
        val GREETING_REPLIES = listOf(
            "Heyy! 👋 How's your week going?",
            "Hi! So nice to hear from you 😊",
            "Hello hello! What have you been up to?",
        )
        val GENERIC_REPLIES = listOf(
            "Haha love that 😄",
            "Totally agree!",
            "That's so cool, tell me more!",
            "Ha, same here honestly.",
            "Oh nice, that sounds like a great time.",
        )
        val INTEREST_FOLLOW_UPS = listOf(
            "Also — done any {interest} lately?",
            "We should do something {interest}-related soon!",
            "By the way, any {interest} recommendations?",
        )
        val WAVE_BACK_WITH_INTEREST = listOf(
            "Hey! 👋 Thanks for the wave — always happy to meet another {interest} person.",
            "Hi! Saw we both love {interest}. Let's plan something soon?",
            "Wave received 😄 Fellow {interest} fan! How long have you been into it?",
        )
        val WAVE_BACK_GENERIC = listOf(
            "Hey! 👋 Thanks for the wave. How long have you lived around here?",
            "Hi there! Always nice to meet new people near {neighborhood} 😊",
        )
    }
}
