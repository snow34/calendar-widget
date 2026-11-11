package io.github.snow34.calendarwidget.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class PendingEventsTest {
    private val now = 1_800_000_000_000L
    private fun pending(uid: String, cal: String = "work", start: String = "2026-11-12T07:00:00Z", created: Long = now) = PendingEvent(
        calendarId = cal, uid = uid, title = uid, location = null, allDay = false,
        start = Instant.parse(start), end = Instant.parse(start).plusSeconds(3600), createdAtMillis = created,
    )
    private fun occ(uid: String, cal: String = "work", start: String = "2026-11-12T08:00:00Z") = Occurrence(
        uid = uid, summary = uid, location = null, allDay = false,
        start = Instant.parse(start), end = Instant.parse(start).plusSeconds(3600), calendarId = cal,
    )
    private val from = Instant.parse("2026-11-01T00:00:00Z")
    private val to = Instant.parse("2026-12-01T00:00:00Z")

    @Test fun pendingEventsAreShownUntilTheFeedHasThem() {
        val merged = PendingEvents.overlay(listOf(occ("a")), listOf(pending("new")), setOf("work"), from, to)
        assertEquals(listOf("new", "a"), merged.map { it.uid })
        assertEquals("work", merged.first().calendarId)
    }

    @Test fun noDuplicateOnceTheFeedHasIt() {
        val merged = PendingEvents.overlay(listOf(occ("new")), listOf(pending("new")), setOf("work"), from, to)
        assertEquals(1, merged.size)
    }

    @Test fun hiddenWhenCalendarIsOffOrOutsideWindow() {
        assertEquals(0, PendingEvents.overlay(emptyList(), listOf(pending("x")), setOf("home"), from, to).size)
        assertEquals(0, PendingEvents.overlay(emptyList(), listOf(pending("x", start = "2027-01-05T07:00:00Z")), setOf("work"), from, to).size)
    }

    @Test fun pruneDropsSeenOldAndOrphaned() {
        val list = listOf(
            pending("seen"),
            pending("waiting"),
            pending("old", created = now - PendingEvents.MAX_AGE_MILLIS - 1),
            pending("orphan", cal = "deleted-calendar"),
        )
        val kept = PendingEvents.prune(list, mapOf("work" to setOf("seen", "other")), now)
        assertEquals(listOf("waiting"), kept.map { it.uid })
    }

    @Test fun sameUidInAnotherCalendarIsNotAMatch() {
        val merged = PendingEvents.overlay(listOf(occ("new", cal = "home")), listOf(pending("new")), setOf("work", "home"), from, to)
        assertEquals(2, merged.size)
    }
}
