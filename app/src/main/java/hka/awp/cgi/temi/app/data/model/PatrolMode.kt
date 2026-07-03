package hka.awp.cgi.temi.app.data.model

/**
 * Defines the operational modes for the robot's patrol behavior.
 */
enum class PatrolMode {
    /** Patrol routines are dispatched dynamically within a variable time interval. */
    RANDOM,

    /** Patrol routines are dispatched precisely at fixed hourly intervals. */
    FIXED
}
