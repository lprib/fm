@file:Suppress("unused")

package serde

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Patch(val nodes: Array<SerializeNode>, val io: IO)

// TODO store a database of intrinsics somewhere, so this can be more generic
// ie. IO: Hashmap<String, Int?>...
@Serializable
class IO(
    val freq: Int? = null, val gate: Int? = null, val lchan: Int? = null, val rchan: Int? = null
)

sealed class SerializePort {
    abstract val link: Int?
}

@Serializable
class SerializeInPort(val mult: Float, val bias: Float, override val link: Int? = null) :
    SerializePort()

@Serializable
class SerializeOutPort(override val link: Int? = null) : SerializePort()

@Serializable
class SerializeNode(
    val type: String,
    @SerialName("_x") val x: Float,
    @SerialName("_y") val y: Float,
    @SerialName("_name") val customName: String,
    @SerialName("_color") val tintColor: Int,
    val inputPorts: Map<String, SerializeInPort>,
    val outputPorts: Map<String, SerializeOutPort>
)