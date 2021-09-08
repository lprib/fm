package serde

import InputPort
import IntrinsicNode
import Link
import Node
import NodeType
import OutputPort
import Port
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

//val jsonFormatter = Json { prettyPrint = true }
val jsonFormatter = Json

@ExperimentalSerializationApi
fun serializePatch(nodes: List<Node>, links: List<Link>): String {
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

    val serializedNodes = nodes.filter { it !is IntrinsicNode }.map { it.toSerialized(allocations) }
    val io = IO(
        freq = linkValueFromIntrinsic(nodes, NodeType.FREQ, allocations),
        gate = linkValueFromIntrinsic(nodes, NodeType.GATE, allocations),
        lchan = linkValueFromIntrinsic(nodes, NodeType.LCHAN, allocations),
        rchan = linkValueFromIntrinsic(nodes, NodeType.RCHAN, allocations),
    )

    val patch = Patch(serializedNodes.toTypedArray(), io)
    return jsonFormatter.encodeToString(patch)
}

private fun linkValueFromIntrinsic(nodes: List<Node>, type: NodeType, allocations: HashMap<Port, Int>): Int? =
    allocations[nodes.find { it.type == type }?.ports?.first()]


fun InputPort.toSerialized(allocations: HashMap<Port, Int>): SerializeInPort =
    SerializeInPort(multValue, biasValue, allocations[this])

fun OutputPort.toSerialized(allocations: HashMap<Port, Int>): SerializeOutPort =
    SerializeOutPort(allocations[this])

fun Node.toSerialized(allocations: HashMap<Port, Int>): SerializeNode =
    SerializeNode(
        type.typeName,
        location.x,
        location.y,
        customName,
        tintColor,
        inputPorts.associateBy { it.name }.mapValues { (_, v) -> v.toSerialized(allocations) },
        outputPorts.associateBy { it.name }.mapValues { (_, v) -> v.toSerialized(allocations) }
    )


//fun SerializeNode.toNode()