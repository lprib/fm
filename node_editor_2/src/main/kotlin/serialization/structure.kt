package serialization

import InputPort
import Node
import Port
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Patch(val nodes: Array<SerializeNode>)

@Serializable
class IO(
    val freq: Int? = null,
    val gate: Int? = null,
    val lchan: Int? = null,
    val rchan: Int? = null
)

@Serializable
class SerializeInPort(val mult: Float, val bias: Float, val link: Int? = null) {
    companion object {
        fun fromPort(port: InputPort, allocations: HashMap<Port, Int>): SerializeInPort =
            SerializeInPort(port.multValue, port.biasValue, allocations[port])
    }
}

@Serializable
class SerializeOutPort(val link: Int? = null)

@Serializable
sealed class SerializeNode {
    abstract val x: Float
    abstract val y: Float
    abstract val customName: String
    abstract val tintColor: Int
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
) :
    SerializeNode() {

    companion object {
        fun fromNode(node: Node, allocations: HashMap<Port, Int>): SinOsc =
            SinOsc(
                node.location.x,
                node.location.y,
                node.customName,
                node.tintColor,
                SerializeInPort.fromPort(node.inputPorts[0], allocations),
                SerializeInPort.fromPort(node.inputPorts[1], allocations),
                SerializeInPort.fromPort(node.inputPorts[2], allocations),
                SerializeInPort.fromPort(node.inputPorts[3], allocations),
                SerializeOutPort(allocations[node.outputPorts[0]])
            )
    }
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
) :
    SerializeNode() {
    companion object {
        fun fromNode(node: Node, allocations: HashMap<Port, Int>): Adsr =
            Adsr(
                node.location.x,
                node.location.y,
                node.customName,
                node.tintColor,
                SerializeInPort.fromPort(node.inputPorts[0], allocations),
                SerializeInPort.fromPort(node.inputPorts[1], allocations),
                SerializeInPort.fromPort(node.inputPorts[2], allocations),
                SerializeInPort.fromPort(node.inputPorts[3], allocations),
                SerializeInPort.fromPort(node.inputPorts[4], allocations),
                SerializeOutPort(allocations[node.outputPorts[0]])
            )
    }
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
) :
    SerializeNode() {
    companion object {
        fun fromNode(node: Node, allocations: HashMap<Port, Int>): Mixer =
            Mixer(
                node.location.x,
                node.location.y,
                node.customName,
                node.tintColor,
                SerializeInPort.fromPort(node.inputPorts[0], allocations),
                SerializeInPort.fromPort(node.inputPorts[1], allocations),
                SerializeInPort.fromPort(node.inputPorts[2], allocations),
                SerializeInPort.fromPort(node.inputPorts[3], allocations),
                SerializeOutPort(allocations[node.outputPorts[0]])
            )
    }
}
