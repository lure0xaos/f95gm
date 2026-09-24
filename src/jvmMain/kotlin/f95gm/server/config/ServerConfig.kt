package f95gm.server.config

import f95gm.server.config.constants.ServerConstants
import java.nio.file.Path
import kotlin.io.path.isDirectory

internal data class ServerConfig(
    val host: String,
    val port: Int,
    val database: String,
    val staticDirectory: String?,
    val timeouts: TimeoutConfig = TimeoutConfig(),
    val limits: ResourceLimits = ResourceLimits(),
    val traffic: TrafficConfig = TrafficConfig(),
    val retry: RetryConfig = RetryConfig(),
    val deploymentMode: DeploymentMode = DeploymentMode.SINGLE_INSTANCE,
    val openBrowser: Boolean = true
) {
    fun validate() {
        require(host.isNotBlank()) { "Host must not be blank" }
        require(port in ServerConstants.MIN_PORT..ServerConstants.MAX_PORT) { "Port is out of range: $port" }
        timeouts.validate()
        limits.validate(timeouts)
        traffic.validate()
        retry.validate()
        require(deploymentMode == DeploymentMode.SINGLE_INSTANCE) {
            "Unsupported deployment mode: $deploymentMode"
        }
        staticDirectory?.let { directory ->
            val path = Path.of(directory)
            require(path.isDirectory()) { "Static directory does not exist or is not a directory: $directory" }
        }
    }

    companion object {
        fun fromSystemProperties(): ServerConfig = fromSources(
            System.getProperties().stringPropertyNames().associateWith { name -> System.getProperty(name) },
            System.getenv()
        )

        internal fun from(values: Map<String, String>): ServerConfig = fromSources(values, emptyMap())

        internal fun fromSources(values: Map<String, String>, environment: Map<String, String>): ServerConfig =
            fromProperties { name, default -> values[name] ?: environment[environmentName(name)] ?: default }

        private fun fromProperties(getProperty: (String, String) -> String): ServerConfig {
            val properties = ConfigProperties(getProperty)
            return ServerConfig(
                host = properties.text(Keys.HOST),
                port = properties.port(Keys.PORT),
                database = properties.text(Keys.DATABASE).trim(),
                staticDirectory = properties.text(Keys.STATIC_DIRECTORY).trim().takeIf { it.isNotEmpty() },
                timeouts = TimeoutConfig.from(properties),
                limits = ResourceLimits.from(properties),
                traffic = TrafficConfig.from(properties),
                retry = RetryConfig.from(properties),
                deploymentMode = properties.deploymentMode(Keys.DEPLOYMENT_MODE),
                openBrowser = properties.boolean(Keys.OPEN_BROWSER)
            )
        }

        private fun environmentName(propertyName: String): String = "F95GM_" + propertyName
            .removePrefix("f95gm.")
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .uppercase()
    }

    internal fun redactedSummary(): Map<String, Any> = linkedMapOf(
        "host" to host,
        "port" to port,
        "database" to if (database.isBlank()) "<default>" else "<configured>",
        "staticDirectory" to (staticDirectory != null),
        "deploymentMode" to deploymentMode.name,
        "openBrowser" to openBrowser
    )
}

internal enum class DeploymentMode { SINGLE_INSTANCE }

internal class ServerConfigurationException(
    property: String,
    supplied: String,
    expected: String
) : IllegalStateException("Invalid $property='$supplied'; expected $expected")

internal data class RetryConfig(
    val maxAttempts: Int = 3,
    val baseDelayMillis: Millis = Millis(250L),
    val maxDelayMillis: Millis = Millis(30_000L)
) {
    companion object {
        fun from(properties: ConfigProperties) = RetryConfig(
            maxAttempts = properties.positiveInt(Keys.RETRY_MAX_ATTEMPTS),
            baseDelayMillis = Millis(properties.positiveLong(Keys.RETRY_BASE_DELAY)),
            maxDelayMillis = Millis(properties.positiveLong(Keys.RETRY_MAX_DELAY))
        )
    }

    fun validate() {
        require(maxAttempts > 0) { "Retry attempts must be positive" }
        require(baseDelayMillis.value > 0) { "Retry base delay must be positive" }
        require(maxDelayMillis.value >= baseDelayMillis.value) { "Retry maximum delay must not be shorter than base delay" }
    }
}

internal data class TimeoutConfig(
    val requestMillis: Millis = Millis(30_000L),
    val connectMillis: Millis = Millis(10_000L),
    val socketMillis: Millis = Millis(30_000L),
    val sessionIdleMillis: Millis = Millis(ServerConstants.SESSION_IDLE_MILLIS)
) {
    companion object {
        fun from(properties: ConfigProperties) = TimeoutConfig(
            requestMillis = Millis(properties.positiveLong(Keys.REQUEST_TIMEOUT)),
            connectMillis = Millis(properties.positiveLong(Keys.CONNECT_TIMEOUT)),
            socketMillis = Millis(properties.positiveLong(Keys.SOCKET_TIMEOUT)),
            sessionIdleMillis = Millis(properties.positiveLong(Keys.SESSION_IDLE))
        )
    }

    fun validate() {
        require(requestMillis.value > 0) { "Request timeout must be positive" }
        require(connectMillis.value > 0) { "Connect timeout must be positive" }
        require(socketMillis.value > 0) { "Socket timeout must be positive" }
        require(connectMillis.value <= requestMillis.value) { "Connect timeout must not exceed request timeout" }
        require(socketMillis.value <= requestMillis.value) { "Socket timeout must not exceed request timeout" }
        require(sessionIdleMillis.value >= requestMillis.value) { "Session idle timeout must not be shorter than request timeout" }
    }
}

internal data class ResourceLimits(
    val maxSessions: Int = ServerConstants.MAX_SESSIONS,
    val maxRequestBodyBytes: Bytes = Bytes(ServerConstants.MAX_REQUEST_BODY_BYTES)
) {
    companion object {
        fun from(properties: ConfigProperties) = ResourceLimits(
            maxSessions = properties.positiveInt(Keys.MAX_SESSIONS),
            maxRequestBodyBytes = Bytes(properties.positiveLong(Keys.MAX_REQUEST_BODY))
        )
    }

    fun validate(timeouts: TimeoutConfig) {
        require(maxSessions > 0) { "Maximum sessions must be positive" }
        require(maxRequestBodyBytes.value > 0) { "Maximum request body size must be positive" }
        require(timeouts.sessionIdleMillis.value > 0) { "Session idle timeout must be positive" }
    }
}

internal data class TrafficConfig(
    val ipRequestsPerMinute: Int = ServerConstants.API_REQUESTS_PER_MINUTE,
    val accountRequestsPerMinute: Int = ServerConstants.API_REQUESTS_PER_MINUTE / 2,
    val rateLimitWindowMillis: Millis = Millis(ServerConstants.RATE_LIMIT_WINDOW_MILLIS),
    val maxRateLimitBuckets: Int = ServerConstants.MAX_RATE_LIMIT_BUCKETS,
    val circuitBreakerOpenMillis: Millis = Millis(ServerConstants.CIRCUIT_BREAKER_OPEN_MILLIS),
    val maxCircuitBreakerEntries: Int = ServerConstants.MAX_CIRCUIT_BREAKER_ENTRIES
) {
    companion object {
        fun from(properties: ConfigProperties) = TrafficConfig(
            ipRequestsPerMinute = properties.positiveInt(Keys.API_REQUESTS_PER_MINUTE),
            accountRequestsPerMinute = properties.positiveInt(Keys.ACCOUNT_REQUESTS_PER_MINUTE),
            rateLimitWindowMillis = Millis(properties.positiveLong(Keys.RATE_LIMIT_WINDOW)),
            maxRateLimitBuckets = properties.positiveInt(Keys.MAX_RATE_LIMIT_BUCKETS),
            circuitBreakerOpenMillis = Millis(properties.positiveLong(Keys.CIRCUIT_BREAKER_OPEN)),
            maxCircuitBreakerEntries = properties.positiveInt(Keys.MAX_CIRCUIT_BREAKER_ENTRIES)
        )
    }

    fun validate() {
        require(ipRequestsPerMinute > 0) { "IP request limit must be positive" }
        require(accountRequestsPerMinute > 0) { "Account request limit must be positive" }
        require(rateLimitWindowMillis.value > 0) { "Rate-limit window must be positive" }
        require(maxRateLimitBuckets > 0) { "Maximum rate-limit buckets must be positive" }
        require(circuitBreakerOpenMillis.value > 0) { "Circuit-breaker open time must be positive" }
        require(maxCircuitBreakerEntries > 0) { "Maximum circuit-breaker entries must be positive" }
    }
}

internal data class TextKey(val name: String, val default: String)
internal data class IntKey(val name: String, val default: Int)
internal data class LongKey(val name: String, val default: Long)
internal data class BooleanKey(val name: String, val default: Boolean)

internal object Keys {
    val HOST = TextKey("f95gm.host", ServerConstants.DEFAULT_HOST)
    val PORT = IntKey("f95gm.port", ServerConstants.DEFAULT_PORT)
    val DATABASE = TextKey(ServerConstants.DATABASE_PROPERTY, "")
    val STATIC_DIRECTORY = TextKey("f95gm.staticDir", "")
    val REQUEST_TIMEOUT = LongKey("f95gm.requestTimeoutMillis", 30_000L)
    val CONNECT_TIMEOUT = LongKey("f95gm.connectTimeoutMillis", 10_000L)
    val SOCKET_TIMEOUT = LongKey("f95gm.socketTimeoutMillis", 30_000L)
    val SESSION_IDLE = LongKey("f95gm.sessionIdleMillis", ServerConstants.SESSION_IDLE_MILLIS)
    val MAX_SESSIONS = IntKey("f95gm.maxSessions", ServerConstants.MAX_SESSIONS)
    val MAX_REQUEST_BODY = LongKey("f95gm.maxRequestBodyBytes", ServerConstants.MAX_REQUEST_BODY_BYTES)
    val API_REQUESTS_PER_MINUTE = IntKey("f95gm.apiRequestsPerMinute", ServerConstants.API_REQUESTS_PER_MINUTE)
    val ACCOUNT_REQUESTS_PER_MINUTE =
        IntKey("f95gm.accountRequestsPerMinute", ServerConstants.API_REQUESTS_PER_MINUTE / 2)
    val RATE_LIMIT_WINDOW = LongKey("f95gm.rateLimitWindowMillis", ServerConstants.RATE_LIMIT_WINDOW_MILLIS)
    val MAX_RATE_LIMIT_BUCKETS = IntKey("f95gm.maxRateLimitBuckets", ServerConstants.MAX_RATE_LIMIT_BUCKETS)
    val CIRCUIT_BREAKER_OPEN = LongKey("f95gm.circuitBreakerOpenMillis", ServerConstants.CIRCUIT_BREAKER_OPEN_MILLIS)
    val MAX_CIRCUIT_BREAKER_ENTRIES =
        IntKey("f95gm.maxCircuitBreakerEntries", ServerConstants.MAX_CIRCUIT_BREAKER_ENTRIES)
    val RETRY_MAX_ATTEMPTS = IntKey("f95gm.retryMaxAttempts", 3)
    val RETRY_BASE_DELAY = LongKey("f95gm.retryBaseDelayMillis", 250L)
    val RETRY_MAX_DELAY = LongKey("f95gm.retryMaxDelayMillis", 30_000L)
    val DEPLOYMENT_MODE = TextKey("f95gm.deploymentMode", DeploymentMode.SINGLE_INSTANCE.name)
    val OPEN_BROWSER = BooleanKey("f95gm.openBrowser", true)
}

internal class ConfigProperties(private val getProperty: (String, String) -> String) {
    fun text(key: TextKey): String = getProperty(key.name, key.default)

    fun deploymentMode(key: TextKey): DeploymentMode = runCatching {
        DeploymentMode.valueOf(text(key).trim().uppercase())
    }.getOrElse { throw ServerConfigurationException(key.name, text(key), DeploymentMode.entries.joinToString()) }

    fun port(key: IntKey): Int {
        val raw = getProperty(key.name, key.default.toString())
        return raw.toIntOrNull()?.takeIf { it in ServerConstants.MIN_PORT..ServerConstants.MAX_PORT }
            ?: throw ServerConfigurationException(
                key.name,
                raw,
                "an integer between ${ServerConstants.MIN_PORT} and ${ServerConstants.MAX_PORT}"
            )
    }

    fun positiveLong(key: LongKey): Long {
        val raw = getProperty(key.name, key.default.toString())
        return raw.toLongOrNull()?.takeIf { it > 0 }
            ?: throw ServerConfigurationException(key.name, raw, "a positive integer")
    }

    fun positiveInt(key: IntKey): Int {
        val raw = getProperty(key.name, key.default.toString())
        return raw.toIntOrNull()?.takeIf { it > 0 }
            ?: throw ServerConfigurationException(key.name, raw, "a positive integer")
    }

    fun boolean(key: BooleanKey): Boolean =
        getProperty(key.name, key.default.toString()).trim().lowercase().let { raw ->
            when (raw) {
                "true" -> true
                "false" -> false
                else -> throw ServerConfigurationException(key.name, raw, "true or false")
            }
        }
}
