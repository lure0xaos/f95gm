package f95gm.server.state.runtime

import f95gm.server.kernel.ServerInvariantViolation

import f95gm.server.kernel.LifecycleResourceName

/** Validates lifecycle dependency graphs before they can influence close ordering. */
internal object LifecycleDependencyGraph {
    private class InvalidLifecycleDependency(message: String) : ServerInvariantViolation(message)

    internal sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val message: String) : ValidationResult
    }

    fun validateResult(graph: Map<LifecycleResourceName, Set<LifecycleResourceName>>): ValidationResult = try {
        validate(graph)
        ValidationResult.Valid
    } catch (error: InvalidLifecycleDependency) {
        ValidationResult.Invalid(error.message ?: "Invalid lifecycle dependency graph")
    }

    fun validate(graph: Map<LifecycleResourceName, Set<LifecycleResourceName>>) {
        val missing = graph.values.flatten().filterNot(graph::containsKey).toSet()
        if (missing.isNotEmpty()) throw InvalidLifecycleDependency(
            "Lifecycle graph references unregistered resources: $missing"
        )

        val visiting = mutableSetOf<LifecycleResourceName>()
        val visited = mutableSetOf<LifecycleResourceName>()
        fun visit(name: LifecycleResourceName) {
            if (name in visiting) throw InvalidLifecycleDependency(
                "Lifecycle dependency cycle detected at '${name.value}'"
            )
            if (!visited.add(name)) return
            visiting += name
            graph.getValue(name).forEach(::visit)
            visiting -= name
        }
        graph.keys.forEach(::visit)
    }

    /** Returns dependencies first; callers can reverse this for safe shutdown order. */
    fun dependencyFirstOrder(graph: Map<LifecycleResourceName, Set<LifecycleResourceName>>): List<LifecycleResourceName> {
        validate(graph)
        val ordered = mutableListOf<LifecycleResourceName>()
        val visited = mutableSetOf<LifecycleResourceName>()
        fun visit(name: LifecycleResourceName) {
            if (!visited.add(name)) return
            graph.getValue(name).forEach(::visit)
            ordered += name
        }
        graph.keys.forEach(::visit)
        return ordered
    }
}
