package hka.awp.cgi.temi.app.utils

import java.time.Clock
import java.time.format.DateTimeFormatter

fun getLocalTime(clock: Clock, formatter: DateTimeFormatter): String =
    formatter.format(clock.instant())
