package hka.awp.cgi.temi.app.feature.weatherscreen

/**
 * Exception thrown when an error occurs during a communication with the weather API.
 *
 * @property code The error code returned by the API or the HTTP status code.
 * @param message A descriptive message explaining the cause of the error.
 */
class WeatherApiException(val code: Int, message: String) : Exception(message)
