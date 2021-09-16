package serde

import editor.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Defines a collection of linked ports. Since any [InputPort] can only link to a single [OutputPort],
 * This structure holds the list of [InputPort]s ([consumers]) that link to a given [OutputPort] ([producer]).
 */
data class LinkDef(
    var producer: OutputPort? = null, val consumers: ArrayList<InputPort> = arrayListOf()
)

/**
 * Create a complete node graph i.e. node list and link list from given JSON string [json].
 * [windowWidth] is required to right-justify some nodes.
 */
@ExperimentalSerializationApi
fun deserializePatch(json: String, windowWidth: Float): Pair<ArrayList<Node>, ArrayList<Link>> {
    val patch = Json.decodeFromString<Patch>(json)

    // start with pre-initialized intrinsic nodes
    val nodes: ArrayList<Node> = getIntrinsics(windowWidth)
    // This is used as an intermediate storage for links that may not have been fully parsed yet
    val linkLookup = hashMapOf<Int, LinkDef>()

    // populate linkLookup with links to and from intrinsic nodes
    patch.io.freq?.let { createIntrinsicProducer(it, "freq", nodes, linkLookup) }
    patch.io.gate?.let { createIntrinsicProducer(it, "gate", nodes, linkLookup) }
    patch.io.lchan?.let { createIntrinsicConsumer(it, "lchan", nodes, linkLookup) }
    patch.io.rchan?.let { createIntrinsicConsumer(it, "rchan", nodes, linkLookup) }

    // Generate new nodes, and update the linkLookup structure along the way
    for (serializeNode in patch.nodes) {
        val newNode = Node(
            NodeType.fromName(serializeNode.type)!!,
            Vec2(serializeNode.x, serializeNode.y),
            serializeNode.customName,
            serializeNode.tintColor
        )

        // Add entries in linkLookup structure for this node's input ports
        for (inputPort in newNode.inputPorts) {
            val serializeInputPort = serializeNode.inputPorts[inputPort.name]
            serializeInputPort?.let {
                inputPort.multValue = it.mult
                inputPort.biasValue = it.bias
            }
            serializeInputPort?.link?.let { linkLookup.addConsumer(it, inputPort) }
        }

        // Add entries in linkLookup structure for this node's input ports
        for (outputPort in newNode.outputPorts) {
            val linkIdx = serializeNode.outputPorts[outputPort.name]?.link
            linkIdx?.let { linkLookup.setProducer(it, outputPort) }
        }

        nodes += newNode
    }

    val links = arrayListOf<Link>()
    for (linkDef in linkLookup.values) {
        for (consumer in linkDef.consumers) {
            links += Link(consumer, linkDef.producer!!)
        }
    }

    return Pair(nodes, links)
}

/**
 * Update [linkLookup] data structure, such that the intrinsic node of [name] points to [linkIndex].
 * Node [name] must be a producer, i.e. it has a single output port.
 */
private fun createIntrinsicProducer(
    linkIndex: Int,
    name: String,
    nodes: ArrayList<Node>,
    linkLookup: HashMap<Int, LinkDef>,
) = linkLookup.setProducer(
    linkIndex,
    nodes.filterIsInstance<IntrinsicNode>().find { it.customName == name }!!.outputPorts[0]
)

/**
 * Update [linkLookup] data structure, such that the intrinsic node of [name] points to [linkIndex].
 * Node [name] must be a consumer, i.e. it has a single input port.
 */
private fun createIntrinsicConsumer(
    linkIndex: Int,
    name: String,
    nodes: ArrayList<Node>,
    linkLookup: HashMap<Int, LinkDef>,
) = linkLookup.addConsumer(
    linkIndex,
    nodes.filterIsInstance<IntrinsicNode>().find { it.customName == name }!!.inputPorts[0]
)

fun HashMap<Int, LinkDef>.addConsumer(idx: Int, port: InputPort) {
    this.getOrPut(idx) { LinkDef() }
    this[idx]!!.consumers += port
}

fun HashMap<Int, LinkDef>.setProducer(idx: Int, port: OutputPort) {
    this.getOrPut(idx) { LinkDef() }
    this[idx]!!.producer = port
}
