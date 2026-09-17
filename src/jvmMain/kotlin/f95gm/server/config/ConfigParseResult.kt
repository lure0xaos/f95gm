package f95gm.server.config

import f95gm.server.config.ConfigPropertyName

internal sealed interface ConfigParseResult<out T> {
    data class Valid<T>(val value: T) : ConfigParseResult<T>
    data class Invalid(
        val property: ConfigPropertyName,
        val supplied: ConfigurationSuppliedValue,
        val expected: String
    ) : ConfigParseResult<Nothing>
}
