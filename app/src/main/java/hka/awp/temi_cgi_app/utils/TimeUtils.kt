package hka.awp.temi_cgi_app.utils

import java.time.Clock
import java.time.format.DateTimeFormatter


fun getLocalTime(clock: Clock, formatter: DateTimeFormatter): String {
    return formatter.format(clock.instant())
}