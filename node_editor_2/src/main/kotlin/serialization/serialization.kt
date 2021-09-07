package serialization

import Link
import Node
import NodeType
import Port
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@ExperimentalSerializationApi
fun getSerializedString(nodes: List<Node>, links: List<Link>): String {
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
        when (it.type) {
            NodeType.ADSR -> Adsr.fromNode(it, allocations)
            NodeType.SINOSC -> SinOsc.fromNode(it, allocations)
            NodeType.MIXER -> Mixer.fromNode(it, allocations)
            else -> null
        }
    }
    // TODO serialize IO
    val patch = Patch(serializedNodes.toTypedArray())
    return Json.encodeToString(patch)
}