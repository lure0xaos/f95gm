package f95gm.server.config.constants

/** Maximum persisted values and upstream cookie dimensions. */
internal object ServerStorageLimits {
    const val SESSION_ID_LENGTH = 128
    const val ACCOUNT_KEY_LENGTH = 320
    const val COOKIE_NAME_LENGTH = 128
    const val COOKIE_VALUE_LENGTH = 4096
    const val MAX_UPSTREAM_COOKIE_COUNT = 64
    const val MAX_UPSTREAM_COOKIE_BYTES = 32 * 1024
    const val THREAD_ID_LENGTH = 64
    const val TITLE_LENGTH = 512
    const val STORED_VERSION_LENGTH = 128
    const val DEVELOPER_LENGTH = 256
    const val COVER_LENGTH = 2048
}
