package f95gm.client.state.config

internal object ClientConstants {
    const val TOAST_DURATION_MILLIS = 6000L
    const val REQUEST_TIMEOUT_MILLIS = 30_000L
    const val GAME_COVER_WIDTH = 640
    const val GAME_COVER_HEIGHT = 360
    const val JVM_HEALTH_CHECK_INTERVAL_MILLIS = 2000L
    const val VERSION_CHECK_INTERVAL_MILLIS = 15 * 60 * 1000L
    const val FIRST_CHAR_COUNT = 1
    const val NO_RATING = 0.0
    const val RATING_SCALE = 10
    const val RATING_DECIMAL_SCALE = 10.0
    const val ZOOM_STEP = 0.2
    const val MIN_ZOOM = 0.4
    const val MAX_ZOOM = 3.0
    const val BYTE_MASK = 0xff
    const val HEX_RADIX = 16
    const val HEX_DIGITS = 2
    const val SEARCH_IN_KEY = "searchIn"
    const val DATE_LIMIT_KEY = "dateLimit"
}
