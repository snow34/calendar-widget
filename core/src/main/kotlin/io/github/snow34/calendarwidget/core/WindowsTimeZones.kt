package io.github.snow34.calendarwidget.core

/**
 * Outlook/Exchange calendar feeds name time zones the Windows way ("W. Europe Standard Time").
 * Maps the common ones to IANA ids; unknown names return null (the caller falls back to the phone's zone).
 * Zones with identical rules may be represented by a different city than Windows' reference city.
 */
object WindowsTimeZones {
    val all: Map<String, String> = mapOf(
        "UTC" to "UTC",
        "GMT Standard Time" to "Europe/London",
        "Greenwich Standard Time" to "Atlantic/Reykjavik",
        "W. Europe Standard Time" to "Europe/Berlin",
        "Central Europe Standard Time" to "Europe/Budapest",
        "Central European Standard Time" to "Europe/Warsaw",
        "Romance Standard Time" to "Europe/Paris",
        "E. Europe Standard Time" to "Europe/Chisinau",
        "FLE Standard Time" to "Europe/Helsinki",
        "GTB Standard Time" to "Europe/Bucharest",
        "Russian Standard Time" to "Europe/Moscow",
        "Turkey Standard Time" to "Europe/Istanbul",
        "Israel Standard Time" to "Asia/Jerusalem",
        "Egypt Standard Time" to "Africa/Cairo",
        "South Africa Standard Time" to "Africa/Johannesburg",
        "Arabian Standard Time" to "Asia/Dubai",
        "India Standard Time" to "Asia/Kolkata",
        "SE Asia Standard Time" to "Asia/Bangkok",
        "China Standard Time" to "Asia/Shanghai",
        "Singapore Standard Time" to "Asia/Singapore",
        "Tokyo Standard Time" to "Asia/Tokyo",
        "Korea Standard Time" to "Asia/Seoul",
        "W. Australia Standard Time" to "Australia/Perth",
        "AUS Central Standard Time" to "Australia/Darwin",
        "Cen. Australia Standard Time" to "Australia/Adelaide",
        "E. Australia Standard Time" to "Australia/Brisbane",
        "AUS Eastern Standard Time" to "Australia/Sydney",
        "New Zealand Standard Time" to "Pacific/Auckland",
        "Hawaiian Standard Time" to "Pacific/Honolulu",
        "Alaskan Standard Time" to "America/Anchorage",
        "Pacific Standard Time" to "America/Los_Angeles",
        "US Mountain Standard Time" to "America/Phoenix",
        "Mountain Standard Time" to "America/Denver",
        "Central Standard Time" to "America/Chicago",
        "Canada Central Standard Time" to "America/Regina",
        "Central America Standard Time" to "America/Guatemala",
        "Mexico Standard Time" to "America/Mexico_City",
        "Eastern Standard Time" to "America/New_York",
        "SA Pacific Standard Time" to "America/Bogota",
        "Atlantic Standard Time" to "America/Halifax",
        "Newfoundland Standard Time" to "America/St_Johns",
        "E. South America Standard Time" to "America/Sao_Paulo",
        "Argentina Standard Time" to "America/Argentina/Buenos_Aires",
    )

    fun toIana(windowsName: String): String? = all[windowsName.trim()]
}
