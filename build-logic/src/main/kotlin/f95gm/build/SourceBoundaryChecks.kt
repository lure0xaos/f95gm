package f95gm.build

/** Pure source policies shared by Gradle tasks and executable violation fixtures. */
internal object SourceBoundaryChecks {
    private const val CLIENT_ROOT = "src/jsMain/kotlin/f95gm/client/"
    private const val TRANSPORT_ROOT = "${CLIENT_ROOT}data/api/"
    private const val RATE_LIMITER = "src/jvmMain/kotlin/f95gm/server/http/request/ratelimit/RequestRateLimiter.kt"
    private val transportCall = Regex("\\btransport\\s*\\.\\s*request\\s*\\(|\\brequestJsonEnvelope\\s*\\(")

    fun apiViolations(path: String, text: String): List<String> {
        val relative = sourcePolicyPath(path)
        if (relative.startsWith(TRANSPORT_ROOT)) return emptyList()
        val imports = if (!relative.startsWith("src/jsMain/kotlin/") ||
            relative == "${CLIENT_ROOT}state/runtime/Http.kt"
        ) emptyList() else
            kotlinImports(text).filter { name ->
                listOf("request", "statement", "engine", "plugins").any { segment ->
                    name == "io.ktor.client.$segment" || name.startsWith("io.ktor.client.$segment.")
                }
            }.map { "$relative: import $it" }
        val code = kotlinCode(text)
        return imports + transportCall.findAll(code).map { match ->
            "$relative:${lineNumber(code, match.range.first)}: transport call must stay in the API adapter"
        }.toList()
    }

    fun typedViolations(path: String, text: String): List<String> {
        val relative = sourcePolicyPath(path)
        val violations = mutableListOf<String>()
        if (!BoundarySourcePolicy.isJsonBoundary(path) &&
            "kotlinx.serialization.json.JsonElement" in kotlinImports(text)
        ) {
            violations += "$relative: JsonElement must stay in a wire adapter"
        }
        // windowMillis remains the duration compatibility API; it cannot exempt raw current-time values.
        if (relative == RATE_LIMITER) {
            val code = kotlinCode(text)
            Regex("\\bnowMillis\\b").findAll(code).forEach { match ->
                violations += "$relative:${lineNumber(code, match.range.first)}: raw rate-limit timestamps are forbidden"
            }
        }
        return violations
    }

    fun controllerViolations(path: String, text: String): List<String> {
        val relative = sourcePolicyPath(path)
        val pattern = when {
            relative.startsWith("${CLIENT_ROOT}actions/") -> Regex("\\bClientController\\b|\\bcontroller\\b")
            relative == "${CLIENT_ROOT}state/runtime/ClientCapabilities.kt" -> Regex("\\bClientController\\b")
            else -> return emptyList()
        }
        val code = kotlinCode(text)
        return pattern.findAll(code).map { match ->
            "$relative:${lineNumber(code, match.range.first)}: feature capabilities must not expose the controller"
        }.toList()
    }

    private fun lineNumber(code: String, offset: Int): Int = code.take(offset).count { it == '\n' } + 1
}
