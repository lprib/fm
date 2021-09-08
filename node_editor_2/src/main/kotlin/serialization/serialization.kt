package serialization

import InputPort
import Link
import Node
import NodeType
import OutputPort
import Port
import Vec2
import getIntrinsics
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


private val jsonFormatter = Json { prettyPrint = true }

@ExperimentalSerializationApi
fun serializePath(nodes: List<Node>, links: List<Link>): String {
    // contains the mapping from port to programState index identifier
    val allocations = hashMapOf<Port, Int>()
    // intermediate: used to generate new indices (they are indices into this arraylist)
    val portIndices = arrayListOf<Port>()
    for (link in links) {
        // allocate link based on it's output port. Since output ports can only
        // be connected to a single other port, their allocation number will be unique
        if (!portIndices.contains(link.outputPort)) {
            portIndices.add(link.outputPort)
        }
        val allocationIndex = portIndices.indexOf(link.outputPort)
        allocations[link.outputPort] = allocationIndex
        allocations[link.inputPort] = allocationIndex
    }

    val serializedNodes = nodes.mapNotNull {
//        when (it.type) {
//            NodeType.ADSR -> Adsr.fromNode(it, allocations)
//            NodeType.SINOSC -> SinOsc.fromNode(it, allocations)
//            NodeType.MIXER -> Mixer.fromNode(it, allocations)
//            else -> null
//        }
    }
    val io = IO(
        freq = linkValueFromIntrinsic(nodes, NodeType.FREQ, allocations),
        gate = linkValueFromIntrinsic(nodes, NodeType.GATE, allocations),
        lchan = linkValueFromIntrinsic(nodes, NodeType.LCHAN, allocations),
        rchan = linkValueFromIntrinsic(nodes, NodeType.RCHAN, allocations),
    )

//    val patch = Patch(serializedNodes.toTypedArray(), io)
//    return jsonFormatter.encodeToString(patch)
    TODO()
}

private fun linkValueFromIntrinsic(nodes: List<Node>, type: NodeType, allocations: HashMap<Port, Int>): Int? =
    allocations[nodes.find { it.type == type }?.ports?.first()]

data class LinkDef(var producer: OutputPort? = null, val consumers: ArrayList<InputPort> = arrayListOf())

@ExperimentalSerializationApi
fun deserializePath(json: String, windowWidth: Float): Pair<ArrayList<Node>, ArrayList<Link>> {
    val patch = jsonFormatter.decodeFromString<Patch>(json)

    val nodes: ArrayList<Node> = getIntrinsics(windowWidth)
    val linkLookup = hashMapOf<Int, LinkDef>()

    patch.io.freq?.let { createIntrinsicProducer(it, NodeType.FREQ, nodes, linkLookup) }
    patch.io.gate?.let { createIntrinsicProducer(it, NodeType.GATE, nodes, linkLookup) }
    patch.io.lchan?.let { createIntrinsicConsumer(it, NodeType.LCHAN, nodes, linkLookup) }
    patch.io.rchan?.let { createIntrinsicConsumer(it, NodeType.RCHAN, nodes, linkLookup) }

    nodes.addAll(patch.nodes.map { it.toNode(linkLookup) })
    TODO()
}

// Update LinkLookup data structure, such that the intrinsic node of `type` points to the link index idx
private fun createIntrinsicProducer(
    linkIdx: Int,
    type: NodeType,
    nodes: ArrayList<Node>,
    linkLookup: HashMap<Int, LinkDef>
) {
    linkLookup.getOrPut(linkIdx) { LinkDef() }
    linkLookup[linkIdx]!!.producer = nodes.find { it.type == type }!!.outputPorts[0]
}

// Update LinkLookup data structure, such that the intrinsic node of `type` points to the link index idx
private fun createIntrinsicConsumer(
    linkIdx: Int,
    type: NodeType,
    nodes: ArrayList<Node>,
    linkLookup: HashMap<Int, LinkDef>
) {
    linkLookup.getOrPut(linkIdx) { LinkDef() }
    linkLookup[linkIdx]!!.consumers.add(nodes.find { it.type == type }!!.inputPorts[0])
}

fun SerializeNode.toNode(linkLookup: HashMap<Int, LinkDef>): Node {
    val node = when (this) {
        is SinOsc -> Node(NodeType.SINOSC, Vec2(x, y), customName, tintColor)
        is Adsr -> Node(NodeType.ADSR, Vec2(x, y), customName, tintColor)
        is Mixer -> Node(NodeType.MIXER, Vec2(x, y), customName, tintColor)
    }
    TODO("LINK PORTS")
}

fun InputPort.toSerialized(allocations: HashMap<Port, Int>): SerializeInPort =
    SerializeInPort(multValue, biasValue, allocations[this])

fun Node.toSerializeNode(allocations: HashMap<Port, Int>): SerializeNode {
    when (type) {
        NodeType.ADSR -> SinOsc(
            location.x,
            location.y,
            customName,
            tintColor,
            inputPorts[0].toSerialized(allocations),
            inputPorts[1].toSerialized(allocations),
            inputPorts[2].toSerialized(allocations),
            inputPorts[3].toSerialized(allocations)
        )
        NodeType.SINOSC -> TODO()
        NodeType.MIXER -> TODO()
        else -> throw IllegalArgumentException("Serialize node must not be an intrinsic node")
    }
}

//fun SerializeNode.toNode()