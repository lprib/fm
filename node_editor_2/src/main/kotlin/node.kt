import processing.core.PApplet
import java.awt.Color
import javax.swing.JColorChooser
import javax.swing.JOptionPane
import kotlin.math.max

object NodeDrawOptions {
    const val width = 200f
    private const val portPadding = 10f
    const val portSpacing = PortDrawOptions.portHeight + portPadding
    const val titleUnderlineY = 2f * DrawOptions.textSize + 13f
    const val portStartY = titleUnderlineY + portSpacing / 2f
    const val tintAlpha = 40
    const val titleTextSize = DrawOptions.textSize + 2f
}

data class PortDescription(val name: String, val defaultBias: Float, val input: Boolean) {
    fun toPort(parent: Node, location: Vec2): Port =
        if (input) InputPort(parent, name, location, 1f, defaultBias) else OutputPort(parent, name, location)
}

enum class NodeType(
    val typeName: String,
    val ports: Array<PortDescription>,
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
        "sinosc",
        arrayOf(
            PortDescription("freq", 0f, true),
            PortDescription("phase", 0f, true),
            PortDescription("vol", 1f, true),
            PortDescription("feedback", 0f, true),
            PortDescription("out", 0f, false),
        )
    ),
    MIXER(
        "mixer",
        arrayOf(
            PortDescription("in1", 0f, true),
            PortDescription("in2", 0f, true),
            PortDescription("in3", 0f, true),
            PortDescription("in4", 0f, true),
            PortDescription("out", 0f, false),
        )
    ),

    // intrinsics
    FREQ("freq", arrayOf(PortDescription("out", 0f, false))),
    GATE("gate", arrayOf(PortDescription("out", 0f, false))),
    RCHAN("rchan", arrayOf(PortDescription("in", 0f, true))),
    LCHAN("lchan", arrayOf(PortDescription("in", 0f, true)));

    companion object {
        fun fromName(name: String): NodeType? = values().find { it.typeName == name }
    }
}

open class Node(
    val type: NodeType,
    var location: Vec2 = Vec2(0f, 0f),
    var customName: String = "",
    var tintColor: Int = DrawOptions.nodeFillColor
) :
    SelectableObject {
    override var selected = false
    open var mouseSnapped = false
    private val width = NodeDrawOptions.width
    private val height = max(
        type.ports.filter { it.input }.size,
        type.ports.filter { !it.input }.size
    ) * NodeDrawOptions.portSpacing + NodeDrawOptions.titleUnderlineY

    val inputPorts: List<InputPort> = type.ports.filter { it.input }.withIndex().map { (idx, p) ->
        p.toPort(
            this,
            Vec2(0f, NodeDrawOptions.portStartY + idx.toFloat() * NodeDrawOptions.portSpacing)
        ) as InputPort
    }

    val outputPorts: List<OutputPort> = type.ports.filter { !it.input }.withIndex().map { (idx, p) ->
        p.toPort(
            this,
            Vec2(width, NodeDrawOptions.portStartY + idx.toFloat() * NodeDrawOptions.portSpacing)
        ) as OutputPort
    }
    val ports: Iterable<Port> get() = inputPorts.asIterable().plus(outputPorts.asIterable())

    open fun editCustomName(p: PApplet) {
        val input: String? = JOptionPane.showInputDialog(p.frame, "Enter name")
        input?.let { customName = input }
    }

    open fun editTintColor(p: PApplet) {
        val awtColor: Color? = JColorChooser.showDialog(p.frame, "Node Color", Color(DrawOptions.nodeFillColor))
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

        p.strokeWeight(1f)
        p.stroke(DrawOptions.uiColor)
        p.fill(DrawOptions.nodeFillColor)

        drawOutlineRect(p)
        p.noStroke()
        p.fill((tintColor and 0xFFFFFF) or (NodeDrawOptions.tintAlpha shl 24))
        drawOutlineRect(p)
        if (selected) {
            p.fill(DrawOptions.highlightOverlayColor)
            drawOutlineRect(p)
        }
        p.stroke(DrawOptions.uiColor)

        p.line(0f, NodeDrawOptions.titleUnderlineY, width, NodeDrawOptions.titleUnderlineY)

        p.textAlign(PApplet.CENTER, PApplet.TOP)
        p.fill(DrawOptions.nodeTitleColor)
        p.textSize(NodeDrawOptions.titleTextSize)
        p.text(customName, width / 2f, 0f)
        p.fill(DrawOptions.uiColor)
        p.textSize(DrawOptions.textSize)
        p.text(type.typeName, width / 2f, NodeDrawOptions.titleTextSize + 4f)

        ports.forEach {
            it.draw(p)
        }

        p.popMatrix()
    }

    private fun drawOutlineRect(p: PApplet) {
        p.rect(0f, 0f, width, height, 5f, 5f, 5f, 5f)
    }
}

class IntrinsicNode(type: NodeType, location: Vec2) : Node(type, location, "", DrawOptions.intrinsicTintColor) {
    // Do nothing on mouseSnapped set, because these nodes cannot be moved
    // TODO add notification popups for all of these actions
    override var mouseSnapped: Boolean
        get() = false
        set(value) {}

    override fun editCustomName(p: PApplet) {}
    override fun editTintColor(p: PApplet) {}
}

fun getIntrinsics(windowWidth: Float): ArrayList<Node> =
    arrayListOf(
        IntrinsicNode(NodeType.FREQ, Vec2(20f, 20f)),
        IntrinsicNode(NodeType.GATE, Vec2(20f, 120f)),
        IntrinsicNode(NodeType.LCHAN, Vec2(windowWidth - NodeDrawOptions.width - 20f, 20f)),
        IntrinsicNode(NodeType.RCHAN, Vec2(windowWidth - NodeDrawOptions.width - 20f, 120f)),
    )