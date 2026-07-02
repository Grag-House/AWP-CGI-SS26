package hka.awp.cgi.temi.app.utils

import java.time.Clock
import java.time.format.DateTimeFormatter

/**
 * Utility functions for handling time-related operations.
 */

/**
 * Formats the current time from the provided [Clock] using the given [DateTimeFormatter].
 *
 * @param clock The clock to retrieve the current instant from.
 * @param formatter The formatter to use for the output string.
 * @return A formatted string representing the current time.
 */
fun getLocalTime(clock: Clock, formatter: DateTimeFormatter): String =
    formatter.format(clock.instant())
