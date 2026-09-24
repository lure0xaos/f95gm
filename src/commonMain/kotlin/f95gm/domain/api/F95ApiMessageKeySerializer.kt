package f95gm.domain.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object F95ApiMessageKeySerializer : KSerializer<F95ApiMessageKey> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("F95ApiMessageKey", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: F95ApiMessageKey) = encoder.encodeString(value.value)

    override fun deserialize(decoder: Decoder): F95ApiMessageKey = F95ApiMessageKey(decoder.decodeString())
}
