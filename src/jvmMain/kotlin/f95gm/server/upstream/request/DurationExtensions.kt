package f95gm.server.upstream.request

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal fun Duration.Companion.seconds(value: Int): Duration = value.toDuration(DurationUnit.SECONDS)
