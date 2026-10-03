package io.github.ieswar23.buddyup.domain

import com.google.common.truth.Truth.assertThat
import io.github.ieswar23.buddyup.domain.model.Report
import io.github.ieswar23.buddyup.domain.model.ReportReason
import org.junit.Test

class ReportTest {

    @Test
    fun `notes are trimmed and blank notes are dropped`() {
        assertThat(Report.of(ReportReason.SPAM, "  Selling a course  ").note).isEqualTo("Selling a course")
        assertThat(Report.of(ReportReason.SPAM, "   ").note).isNull()
        assertThat(Report.of(ReportReason.OTHER, null)).isEqualTo(Report(ReportReason.OTHER, note = null))
    }

    @Test
    fun `notes are capped at the maximum length`() {
        val report = Report.of(ReportReason.HARASSMENT, "a".repeat(Report.MAX_NOTE_LENGTH + 50))

        assertThat(report.note).hasLength(Report.MAX_NOTE_LENGTH)
    }
}
