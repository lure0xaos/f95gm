package f95gm.server.config

import f95gm.server.config.upstream.UpstreamBaseUrl

/** Pure validation rules for externally supplied configuration values. */
internal object ConfigurationValidation {
    fun upstreamOrigin(configured: String): String {
        return UpstreamBaseUrl.parse(configured).value
    }
}
