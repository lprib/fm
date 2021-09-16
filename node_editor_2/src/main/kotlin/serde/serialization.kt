package serde

import editor.*
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Return the JSON string specifying the given [nodes] and [links] graph structure.
 * The JSON will be serialized in accordance to the [Patch] structure.
 */
@ExperimentalSerializationApi
fun serializePatch(nodes: List<Node>, links: List<Link>): Patch {
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
        freq = linkValueFromIntrinsic(nodes, "freq", allocations),
        gate = linkValueFromIntrinsic(nodes, "gate", allocations),
        lchan = linkValueFromIntrinsic(nodes, "lchan", allocations),
        rchan = linkValueFromIntrinsic(nodes, "rchan", allocations),
    )

    return Patch(serializedNodes.toTypedArray(), io)
}

/**
 * Find the intrinsic node of [name] in the list [nodes]. Get its link allocation index from the [allocations] map.
 */
private fun linkValueFromIntrinsic(
    nodes: List<Node>, name: String, allocations: HashMap<Port, Int>
): Int? = allocations[nodes.filterIsInstance<IntrinsicNode>()
    .find { it.customName == name }?.ports?.first()]

fun InputPort.toSerialized(allocations: HashMap<Port, Int>): SerializeInPort =
    SerializeInPort(multValue, biasValue, allocations[this])

fun OutputPort.toSerialized(allocations: HashMap<Port, Int>): SerializeOutPort =
    SerializeOutPort(allocations[this])

fun Node.toSerialized(allocations: HashMap<Port, Int>): SerializeNode = SerializeNode(type.typeName,
    location.x,
    location.y,
    customName,
    tintColor,
    inputPorts.associateBy { it.name }.mapValues { (_, v) -> v.toSerialized(allocations) },
    outputPorts.associateBy { it.name }.mapValues { (_, v) -> v.toSerialized(allocations) })

//fun SerializeNode.toNode()