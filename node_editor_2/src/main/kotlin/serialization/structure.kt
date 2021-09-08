package serialization

import NodeType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Patch(val nodes: Array<SerializeNode>, val io: IO)

@Serializable
class IO(
    val freq: Int? = null,
    val gate: Int? = null,
    val lchan: Int? = null,
    val rchan: Int? = null
)

sealed class SerializePort {
    abstract val link: Int?
}

@Serializable
class SerializeInPort(val mult: Float, val bias: Float, override val link: Int? = null) : SerializePort()

@Serializable
class SerializeOutPort(override val link: Int? = null) : SerializePort()

@Serializable
sealed class SerializeNode {
    abstract val x: Float
    abstract val y: Float
    abstract val customName: String
    abstract val tintColor: Int

    abstract val ports: Iterator<SerializePort>
    abstract val type: NodeType
}

@Serializable
@SerialName("sinosc")
class SinOsc(
    @SerialName("_x")
    override val x: Float,
    @SerialName("_y")
    override val y: Float,
    @SerialName("_name")
    override val customName: String,
    @SerialName("_color")
    override val tintColor: Int,
    val freq: SerializeInPort,
    val phase: SerializeInPort,
    val vol: SerializeInPort,
    val feedback: SerializeInPort,
    val out: SerializeOutPort
) : SerializeNode() {
    override val ports: Iterator<SerializePort>
        get() = listOf(freq, phase, vol, feedback, out).iterator()
    override val type: NodeType get() = NodeType.SINOSC
}

@Serializable
@SerialName("adsr")
class Adsr(
    @SerialName("_x")
    override val x: Float,
    @SerialName("_y")
    override val y: Float,
    @SerialName("_name")
    override val customName: String,
    @SerialName("_color")
    override val tintColor: Int,
    val gate: SerializeInPort,
    val a: SerializeInPort,
    val d: SerializeInPort,
    val s: SerializeInPort,
    val r: SerializeInPort,
    val out: SerializeOutPort
) : SerializeNode() {
    override val ports: Iterator<SerializePort>
        get() = listOf(gate, a, d, s, r, out).iterator()
    override val type: NodeType get() = NodeType.ADSR
}

@Serializable
@SerialName("mixer")
class Mixer(
    @SerialName("_x")
    override val x: Float,
    @SerialName("_y")
    override val y: Float,
    @SerialName("_name")
    override val customName: String,
    @SerialName("_color")
    override val tintColor: Int,
    val in1: SerializeInPort,
    val in2: SerializeInPort,
    val in3: SerializeInPort,
    val in4: SerializeInPort,
    val out: SerializeOutPort
) : SerializeNode() {
    override val ports: Iterator<SerializePort>
        get() = listOf(in1, in2, in3, in4, out).iterator()
    override val type: NodeType get() = NodeType.MIXER
}
