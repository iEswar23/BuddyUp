package io.github.ieswar23.buddyup.data

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.buddyup.data.simulation.ReplyGenerator
import io.github.ieswar23.buddyup.data.simulation.ReplyGenerator.Intent
import io.github.ieswar23.buddyup.fakes.person
import org.junit.Test
import kotlin.random.Random

class ReplyGeneratorTest {

    private val generator = ReplyGenerator(Random(42))

    @Test
    fun `classifies messages by intent`() {
        assertThat(generator.classify("Want to grab coffee on Saturday?")).isEqualTo(Intent.PLAN)
        assertThat(generator.classify("What are you reading these days?")).isEqualTo(Intent.QUESTION)
        assertThat(generator.classify("Thanks so much!")).isEqualTo(Intent.THANKS)
        assertThat(generator.classify("Hey there")).isEqualTo(Intent.GREETING)
        assertThat(generator.classify("I finally finished that book")).isEqualTo(Intent.GENERIC)
    }

    @Test
    fun `plan messages get a plan reply`() {
        val reply = generator.replyTo("Coffee this weekend?", person("p1"), listOf("Coffee"))
        assertThat(ReplyGenerator.PLAN_REPLIES).contains(reply)
    }

    @Test
    fun `wave back greeting mentions a shared interest when there is one`() {
        val greeting = generator.waveBackGreeting(person("p1"), listOf("Board games"))
        assertThat(greeting).contains("board games")
    }
}
