package f95gm.domain.api

fun String.parseIntOrNull(): Int? = toIntOrNull()

fun String.parseNonNegativeInt(): Int? = parseIntOrNull()?.takeIf { it >= 0 }

fun String.parsePositiveInt(): Int? = parseIntOrNull()?.takeIf { it > 0 }

fun String.parsePositiveLong(): Long? = trim().toLongOrNull()?.takeIf { it > 0L }

fun String.parseNonNegativeLong(): Long? = trim().toLongOrNull()?.takeIf { it >= 0L }
