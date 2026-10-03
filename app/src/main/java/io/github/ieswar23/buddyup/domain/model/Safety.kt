package io.github.ieswar23.buddyup.domain.model

/** Why the user reported someone. Reporting is optional when blocking. */
enum class ReportReason(val label: String) {
    SPAM("Spam or selling something"),
    FAKE_PROFILE("Fake profile or impersonation"),
    INAPPROPRIATE("Inappropriate messages"),
    HARASSMENT("Harassment or bullying"),
    NOT_PLATONIC("Looking for dates, not friends"),
    OTHER("Something else"),
}

/** A report filed alongside a block: a [reason] plus an optional free-text [note]. */
data class Report(val reason: ReportReason, val note: String? = null) {
    companion object {
        const val MAX_NOTE_LENGTH = 300

        /** Trims the note, drops it when blank and caps it at [MAX_NOTE_LENGTH] characters. */
        fun of(reason: ReportReason, note: String?): Report =
            Report(reason, note?.trim()?.take(MAX_NOTE_LENGTH)?.ifEmpty { null })
    }
}

/** Someone the user blocked. Blocked people are hidden from Discover, Waves, Friends and chats. */
data class BlockedPerson(
    val person: Person,
    val report: Report?,
    val blockedAt: Long,
)
