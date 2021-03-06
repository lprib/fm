package editor

import processing.core.PApplet
import java.awt.Color
import javax.swing.JColorChooser
import javax.swing.JOptionPane
import kotlin.math.max

object NodeDrawOptions {
    /**
     * Width of each node
     */
    const val width = 150f

    /**
     * Vertical space between ports
     */
    private const val portPadding = 0f

    /**
     * Vertical spacing from center to center of ports
     */
    const val portSpacing = PortDrawOptions.portHeight + portPadding

    /**
     * Vertical distance down from top of node where the underline separator between title and ports will be drawn.
     */
    const val titleBlockHeight = 2f * DrawOptions.textSize + 13f

    /**
     * Padding between bottom of title block and start of first port, and between last port and bottom of node
     */
    const val portBlockVertPadding = 5f

    /**
     * Y value down from top of node where the first port will be drawn.
     * This is distance from top edge to center of the port.
     */
    const val portStartY = titleBlockHeight + portSpacing / 2f + portBlockVertPadding

    /**
     * Alpha value that a node's tint will be given when it is overlayed
     */
    const val tintAlpha = 40

    /**
     * Text size of a node's title / [Node.customName]
     */
    const val titleTextSize = DrawOptions.textSize + 2f
}

data class PortDescription(val name: String, val defaultBias: Float, val input: Boolean) {
    fun toPort(parent: Node, location: Vec2): Port =
        if (input) InputPort(parent, name, location, 1f, defaultBias) else OutputPort(
            parent, name, location
        )
}

/**
 * [typeName] is used in the JSON serde and used as the unique identifier for this type of node.
 */
enum class NodeType(
    val typeName: String, val ports: Array<PortDescription>
) {
    ADSR(
        "adsr",
        arrayOf(
            PortDescription("gate", 0f, true),
            PortDescription("a", 0f, true),
            PortDescription("d", 0f, true),
            PortDescription("s", 1f, true),
            PortDescription("r", 0f, true),
            PortDescription("out", 0f, false),
        ),
    ),
    SINOSC(
        "sinosc", arrayOf(
            PortDescription("freq", 0f, true),
            PortDescription("phase", 0f, true),
            PortDescription("vol", 0f, true),
            PortDescription("feedback", 0f, true),
            PortDescription("out", 0f, false),
        )
    ),
    MIXER(
        "mixer", arrayOf(
            PortDescription("in1", 0f, true),
            PortDescription("in2", 0f, true),
            PortDescription("mix1", 1f, true),
            PortDescription("mix2", 1f, true),
            PortDescription("out", 0f, false),
        )
    ),

    // intrinsics
    INTRINSIC_IN("input", arrayOf(PortDescription("out", 0f, false))),
    INTRINSIC_OUT("output", arrayOf(PortDescription("in", 0f, true)));

    companion object {
        fun fromName(name: String): NodeType? = values().find { it.typeName == name }
    }
}

/**
 * Node with [type]. Will be displayed at coordinates [location]. [customName] and [tintColor] can be used to add
 * user customizations to this node.
 */
open class Node(
    val type: NodeType,
    var location: Vec2 = Vec2(0f, 0f),
    var customName: String = "",
    var tintColor: Int = DrawOptions.defaultTintColor,
) : SelectableObject {
    override var selected = false
    open var mouseSnapped = false

    private val width = NodeDrawOptions.width
    private val height =
        max(type.ports.filter { it.input }.size, type.ports.filter { !it.input }.size
        ) * NodeDrawOptions.portSpacing + NodeDrawOptions.titleBlockHeight + 2 * NodeDrawOptions.portBlockVertPadding

    /**
     * Initialize input ports from those specified in [type]. Spread Y values over range
     */
    val inputPorts: List<InputPort> = type.ports.filter { it.input }.withIndex().map { (idx, p) ->
        p.toPort(
            this, Vec2(0f, NodeDrawOptions.portStartY + idx.toFloat() * NodeDrawOptions.portSpacing)
        ) as InputPort
    }

    /**
     * Initialize output ports from those specified in [type]. Spread Y values over range
     */
    val outputPorts: List<OutputPort> =
        type.ports.filter { !it.input }.withIndex().map { (idx, p) ->
            p.toPort(
                this, Vec2(
                    width, NodeDrawOptions.portStartY + idx.toFloat() * NodeDrawOptions.portSpacing
                )
            ) as OutputPort
        }

    /**
     * Iterable that iterates over [inputPorts] concatenated with [outputPorts]
     */
    val ports: Iterable<Port> get() = inputPorts.asIterable().plus(outputPorts.asIterable())

    /**
     * Open a [JOptionPane] that prompts for the custom name
     */
    open fun editCustomName(p: PApplet) {
        val input: String? = JOptionPane.showInputDialog(p.frame, "Enter name")
        input?.let { customName = input }
    }

    /**
     * Open a [JColorChooser] that prompts for the tint color
     */
    open fun editTintColor(p: PApplet) {
        val awtColor: Color? =
            JColorChooser.showDialog(p.frame, "Node Color", Color(DrawOptions.nodeFillColor))
        awtColor?.let { tintColor = awtColor.rgb }
    }

    override fun contains(p: Vec2): Boolean =
        p.x >= location.x && p.y >= location.y && p.x <= location.x + width && p.y <= location.y + height

    override fun draw(p: PApplet) {
        if (mouseSnapped) {
            location = Vec2(p.mouseX - width / 2f, p.mouseY - height / 2f)
        }

        p.pushMatrix()
        p.translate(location.x, location.y)

        // Main rect
        p.strokeWeight(1f)
        p.stroke(DrawOptions.uiColor)
        p.fill(DrawOptions.nodeFillColor)
        drawOutlineRect(p)

        // title block rect
        p.rect(0f, 0f, width, NodeDrawOptions.titleBlockHeight, 5f, 5f, 0f, 0f)
        p.fill((tintColor and 0xFFFFFF) or (NodeDrawOptions.tintAlpha shl 24))
        p.rect(0f, 0f, width, NodeDrawOptions.titleBlockHeight, 5f, 5f, 0f, 0f)

        // Selected highlight overlay
        p.noStroke()
        if (selected) {
            p.fill(DrawOptions.highlightOverlayColor)
            drawOutlineRect(p)
        }

        p.textAlign(PApplet.CENTER, PApplet.TOP)
        p.fill(DrawOptions.nodeTitleColor)
        p.textSize(NodeDrawOptions.titleTextSize)
        p.text(customName, width / 2f, 5f)
        p.fill(DrawOptions.uiColor)
        p.textSize(DrawOptions.textSize)
        p.text(type.typeName, width / 2f, NodeDrawOptions.titleTextSize + 7f)

        ports.forEach {
            it.draw(p)
        }

        p.popMatrix()
    }

    private fun drawOutlineRect(p: PApplet) {
        p.rect(0f, 0f, width, height, 5f, 5f, 5f, 5f)
    }
}

class IntrinsicNode(type: NodeType, location: Vec2, customName: String) :
    Node(type, location, customName, DrawOptions.intrinsicTintColor) {
    /**
     * Override the setter for [mouseSnapped] to do nothing.
     * The getter always returns false, so that the superclass implementation
     * can never attempt to snap this node to the mouse.
     */
    override var mouseSnapped: Boolean
        get() = false
        set(value) {}

    /**
     * Overridden to be a no-op, as this field is non-editable for intrinsics.
     */
    override fun editCustomName(p: PApplet) {}

    /**
     * Overridden to be a no-op, as this field is non-editable for intrinsics.
     */
    override fun editTintColor(p: PApplet) {}
}

/**
 * Return an [ArrayList] of pre-placed intrinsic nodes.
 * [windowWidth] is required as some nodes will be right-justified to the window.
 */
fun getIntrinsics(windowWidth: Float): ArrayList<Node> = arrayListOf(
    IntrinsicNode(NodeType.INTRINSIC_IN, Vec2(20f, 20f), "freq"),
    IntrinsicNode(NodeType.INTRINSIC_IN, Vec2(20f, 120f), "gate"),
    IntrinsicNode(
        NodeType.INTRINSIC_OUT, Vec2(windowWidth - NodeDrawOptions.width - 20f, 20f), "lchan"
    ),
    IntrinsicNode(
        NodeType.INTRINSIC_OUT, Vec2(windowWidth - NodeDrawOptions.width - 20f, 120f), "rchan"
    ),
)