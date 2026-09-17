package f95gm.server.config

internal fun List<ConfigurationIssue>.throwValidation(section: String) {
    if (isNotEmpty()) {
        throw ConfigurationValidationException(
            map { ConfigurationValidationIssue(section, it.message, it.code) }
        )
    }
}

internal fun configurationIssues(block: ConfigurationIssueCollector.() -> Unit): List<ConfigurationIssue> =
    ConfigurationIssueCollector().apply(block).issues()

internal class ConfigurationIssueCollector {
    private val values = mutableListOf<ConfigurationIssue>()

    fun positive(value: Long, message: String) {
        if (value <= 0) values += ConfigurationIssue(message)
    }

    fun atMost(value: Long, maximum: Long, message: String) {
        if (value > maximum) values += ConfigurationIssue(message)
    }

    fun atLeast(value: Long, minimum: Long, message: String) {
        if (value < minimum) values += ConfigurationIssue(message)
    }

    fun addIf(condition: Boolean, issue: ConfigurationIssue) {
        if (condition) values += issue
    }

    fun issues(): List<ConfigurationIssue> = values.toList()
}
