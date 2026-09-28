package f95gm.server.persistence.model

internal fun requireStorageLength(value: String, maxLength: Int, field: String): String {
    require(value.length <= maxLength) { "$field exceeds the database limit of $maxLength characters" }
    return value
}
