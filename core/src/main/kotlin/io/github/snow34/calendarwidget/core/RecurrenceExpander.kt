package io.github.snow34.calendarwidget.core

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Turns parsed events (incl. RRULE series, EXDATEs and RECURRENCE-ID overrides) into concrete occurrences. */
object RecurrenceExpander {

    /** Occurrences overlapping `[from, to)`, sorted by start. */
    fun expand(events: List<RawEvent>, from: Instant, to: Instant): List<Occurrence> {
        val overridesByUid = events.filter { it.recurrenceId != null }.groupBy { it.uid }
        val out = ArrayList<Occurrence>()

        for (e in events) {
            if (e.recurrenceId != null) {
                if (!e.cancelled) addIfInWindow(out, e, e.start, e.end, from, to)
                continue
            }
            if (e.cancelled) continue
            val overridden = overridesByUid[e.uid]?.mapNotNull { it.recurrenceId }?.toSet().orEmpty()
            val rule = e.rrule
            if (rule == null) {
                addIfInWindow(out, e, e.start, e.end, from, to)
                continue
            }
            for (start in occurrenceStarts(e, rule, to)) {
                val instant = start.toInstant()
                if (instant in e.exDates || instant in overridden) continue
                addIfInWindow(out, e, start, endFor(e, start), from, to)
            }
        }
        return out.sortedWith(compareBy({ it.start }, { it.summary }))
    }

    private fun addIfInWindow(
        out: MutableList<Occurrence>, e: RawEvent,
        start: ZonedDateTime, end: ZonedDateTime, from: Instant, to: Instant,
    ) {
        val s = start.toInstant()
        val en = end.toInstant()
        val overlaps = if (s == en) s >= from && s < to else s < to && en > from
        if (overlaps) out += Occurrence(e.uid, e.summary, e.location, e.allDay, s, en)
    }

    private fun endFor(e: RawEvent, start: ZonedDateTime): ZonedDateTime =
        if (e.allDay) {
            val days = ChronoUnit.DAYS.between(e.start.toLocalDate(), e.end.toLocalDate())
            start.toLocalDate().plusDays(days).atStartOfDay(start.zone)
        } else {
            start.plus(Duration.between(e.start, e.end))
        }

    /** Series starts in ascending order, stopping at COUNT / UNTIL / [windowEnd]. */
    private fun occurrenceStarts(e: RawEvent, rule: RecurrenceRule, windowEnd: Instant): Sequence<ZonedDateTime> = sequence {
        val zone = e.start.zone
        val time = e.start.toLocalTime()
        val startDate = e.start.toLocalDate()
        val limit = listOfNotNull(rule.until, windowEnd).min()
        val limitDate = limit.atZone(zone).toLocalDate().plusDays(1)
        var emitted = 0

        for (k in 0 until MAX_PERIODS) {
            val (periodStart, dates) = period(rule, startDate, k.toLong() * rule.interval)
            if (periodStart > limitDate) return@sequence
            for (d in dates) {
                if (d < startDate) continue
                val start = if (e.allDay) d.atStartOfDay(zone) else ZonedDateTime.of(d, time, zone)
                if (start.toInstant() > limit) return@sequence
                yield(start)
                emitted++
                if (rule.count != null && emitted >= rule.count) return@sequence
            }
        }
    }

    /** Start of the k-th period and the matching dates inside it, ascending. */
    private fun period(rule: RecurrenceRule, startDate: LocalDate, offset: Long): Pair<LocalDate, List<LocalDate>> {
        val months = rule.byMonth
        fun monthOk(d: LocalDate) = months.isEmpty() || d.monthValue in months
        return when (rule.freq) {
            Frequency.DAILY -> {
                val d = startDate.plusDays(offset)
                val ok = monthOk(d) && dayOk(rule, d) && (rule.byMonthDay.isEmpty() || monthDayOk(rule, d))
                d to (if (ok) listOf(d) else emptyList())
            }
            Frequency.WEEKLY -> {
                val weekStart = startDate.with(TemporalAdjusters.previousOrSame(rule.weekStart)).plusWeeks(offset)
                val days = rule.byDay.map { it.day }.ifEmpty { listOf(startDate.dayOfWeek) }.toSet()
                weekStart to (0L..6L).map { weekStart.plusDays(it) }.filter { it.dayOfWeek in days && monthOk(it) }
            }
            Frequency.MONTHLY -> {
                val ym = YearMonth.from(startDate).plusMonths(offset)
                ym.atDay(1) to monthDates(rule, ym, startDate).filter(::monthOk)
            }
            Frequency.YEARLY -> {
                val year = startDate.year + offset.toInt()
                val ms = months.ifEmpty { listOf(startDate.monthValue) }
                LocalDate.of(year, 1, 1) to ms.flatMap { monthDates(rule, YearMonth.of(year, it), startDate) }.sorted()
            }
        }
    }

    private fun dayOk(rule: RecurrenceRule, d: LocalDate) =
        rule.byDay.isEmpty() || rule.byDay.any { it.day == d.dayOfWeek }

    private fun monthDayOk(rule: RecurrenceRule, d: LocalDate): Boolean {
        val len = d.lengthOfMonth()
        return rule.byMonthDay.any { (if (it > 0) it else len + it + 1) == d.dayOfMonth }
    }

    private fun monthDates(rule: RecurrenceRule, ym: YearMonth, startDate: LocalDate): List<LocalDate> {
        val len = ym.lengthOfMonth()
        val dates: List<LocalDate> = when {
            rule.byMonthDay.isNotEmpty() ->
                rule.byMonthDay.map { if (it > 0) it else len + it + 1 }
                    .filter { it in 1..len }.map { ym.atDay(it) }
                    .filter { dayOk(rule, it) }
            rule.byDay.isNotEmpty() -> rule.byDay.flatMap { nthWeekdays(ym, it) }
            else -> if (startDate.dayOfMonth <= len) listOf(ym.atDay(startDate.dayOfMonth)) else emptyList()
        }
        return dates.distinct().sorted()
    }

    private fun nthWeekdays(ym: YearMonth, bd: ByDay): List<LocalDate> {
        val all = (1..ym.lengthOfMonth()).map { ym.atDay(it) }.filter { it.dayOfWeek == bd.day }
        val n = bd.ordinal ?: return all
        val idx = if (n > 0) n - 1 else all.size + n
        return listOfNotNull(all.getOrNull(idx))
    }

    private const val MAX_PERIODS = 200_000
}
