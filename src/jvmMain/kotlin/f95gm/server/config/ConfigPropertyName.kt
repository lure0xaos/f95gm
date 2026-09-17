package f95gm.server.config

/** Identifies a configuration property without exposing raw strings at boundaries. */
@JvmInline
internal value class ConfigPropertyName(val value: String) {
    init { require(value.isNotBlank()) }
}
