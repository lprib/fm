package serde

import InputPort
import Link
import Node
import NodeType
import OutputPort
import Vec2
import getIntrinsics
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString

data class LinkDef(var producer: OutputPort? = null, val consumers: ArrayList<InputPort> = arrayListOf())

@ExperimentalSerializationApi
fun deserializePatch(json: String, windowWidth: Float): Pair<ArrayList<Node>, ArrayList<Link>> {
    val patch = jsonFormatter.decodeFromString<Patch>(json)

    val nodes: ArrayList<Node> = getIntrinsics(windowWidth)
    val linkLookup = hashMapOf<Int, LinkDef>()

    patch.io.freq?.let { createIntrinsicProducer(it, NodeType.FREQ, nodes, linkLookup) }
    patch.io.gate?.let { createIntrinsicProducer(it, NodeType.GATE, nodes, linkLookup) }
    patch.io.lchan?.let { createIntrinsicConsumer(it, NodeType.LCHAN, nodes, linkLookup) }
    patch.io.rchan?.let { createIntrinsicConsumer(it, NodeType.RCHAN, nodes, linkLookup) }

    // Generate new nodes, and update the linkLookup structure along the way
    for (serializeNode in patch.nodes) {
        val newNode = Node(
            NodeType.fromName(serializeNode.type)!!,
            Vec2(serializeNode.x, serializeNode.y),
            serializeNode.customName,
            serializeNode.tintColor
        )
        for (inputPort in newNode.inputPorts) {
            val linkIdx = serializeNode.inputPorts[inputPort.name]?.link
            linkIdx?.let {
                linkLookup.getOrPut(it) { LinkDef() }
                linkLookup[it]!!.consumers += inputPort
            }
        }

        for (port in newNode.outputPorts) {
            val linkIdx = serializeNode.outputPorts[port.name]?.link
            linkIdx?.let {
                //TODO extract into extension func
                linkLookup.getOrPut(it) { LinkDef() }
                linkLookup[it]!!.producer = port
            }
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

// Update LinkLookup data structure, such that the intrinsic node of `type` points to the link index idx
private fun createIntrinsicProducer(
    linkIdx: Int,
    type: NodeType,
    nodes: ArrayList<Node>,
    linkLookup: HashMap<Int, LinkDef>
) = linkLookup.setProducer(linkIdx, nodes.find { it.type == type }!!.outputPorts[0])

// Update LinkLookup data structure, such that the intrinsic node of `type` points to the link index idx
private fun createIntrinsicConsumer(
    linkIdx: Int,
    type: NodeType,
    nodes: ArrayList<Node>,
    linkLookup: HashMap<Int, LinkDef>
) = linkLookup.addConsumer(linkIdx, nodes.find { it.type == type }!!.inputPorts[0])

fun HashMap<Int, LinkDef>.addConsumer(idx: Int, port: InputPort) {
    this.getOrPut(idx) { LinkDef() }
    this[idx]!!.consumers += port
}

fun HashMap<Int, LinkDef>.setProducer(idx: Int, port: OutputPort) {
    this.getOrPut(idx) { LinkDef() }
    this[idx]!!.producer = port
}
