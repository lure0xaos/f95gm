package f95gm.domain.api

import kotlinx.serialization.Serializable

@Serializable(with = F95ApiMessageKeySerializer::class)
data class F95ApiMessageKey(val value: String)
