package f95gm.domain.api

/** Indicates that generated/stable API metadata is internally inconsistent. */
internal class ApiContractInvariantViolation(message: String) : IllegalStateException(message)
