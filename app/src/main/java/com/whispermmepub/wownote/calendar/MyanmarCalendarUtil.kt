package com.whispermmepub.wownote.calendar

import mmcalendar.Astro
import mmcalendar.HolidayCalculator
import mmcalendar.MyanmarDate
import java.time.LocalDate

data class MyanmarDayInfo(
    val westernDate: LocalDate,
    val myanmarYear: String,
    val monthName: String,
    val moonPhase: String,
    val fortnightDay: String,
    val weekDay: String,
    val sabbath: String,
    val holidays: List<String>
) {
    val compactLunar: String
        get() = buildString {
            append(monthName)
            append(" ")
            append(moonPhase)
            if (fortnightDay.isNotBlank()) {
                append(" ")
                append(fortnightDay)
            }
        }
}

object MyanmarCalendarUtil {
    fun info(date: LocalDate): MyanmarDayInfo {
        val md = MyanmarDate.of(date.year, date.monthValue, date.dayOfMonth)
        val astro = Astro.of(md)
        return MyanmarDayInfo(
            westernDate = date,
            myanmarYear = md.year,
            monthName = md.monthName,
            moonPhase = md.moonPhase,
            fortnightDay = md.fortnightDay,
            weekDay = md.weekDay,
            sabbath = astro.sabbath.orEmpty(),
            holidays = runCatching { HolidayCalculator.getHoliday(md).toList() }.getOrDefault(emptyList())
        )
    }

    fun month(year: Int, month: Int): List<MyanmarDayInfo> {
        val first = LocalDate.of(year, month, 1)
        return (1..first.lengthOfMonth()).map { day -> info(first.withDayOfMonth(day)) }
    }
}
