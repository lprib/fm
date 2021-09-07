import processing.core.PApplet
import java.awt.Color
import javax.swing.JColorChooser
import javax.swing.JOptionPane
import kotlin.math.max

object NodeDrawOptions {
    const val width = 125f
    const val titleUnderlineY = 2f * DrawOptions.textSize + 10f
    const val portStartY = 2f * DrawOptions.textSize + 25f
    const val portSpacing = 25
    const val tintAlpha = 40;
    const val titleTextSize = DrawOptions.textSize + 2f
}

data class PortDescription(val name: String, val defaultVal: Float, val input: Boolean) {
    fun toPort(parent: Node, location: Vec2): Port =
        if (input) InputPort(parent, name, location, defaultVal) else OutputPort(parent, name, location)
}

enum class NodeType(
    val nodeName: String,
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
            PortDescription("mult", 1f, true),
            PortDescription("out", 0f, false),
        )
    ),

    // intrinsics
    FREQ("freq", arrayOf(PortDescription("out", 0f, false))),
    GATE("gate", arrayOf(PortDescription("out", 0f, false))),
    RCHAN("rchan", arrayOf(PortDescription("in", 0f, true))),
    LCHAN("lchan", arrayOf(PortDescription("in", 0f, true))),
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
    ) * NodeDrawOptions.portSpacing + NodeDrawOptions.portStartY

    val ports = generatePorts(type)

    open fun editCustomName(p: PApplet) {
        val input: String? = JOptionPane.showInputDialog(p.frame, "Enter name")
        input?.let { customName = input }
    }

    open fun editTintColor(p: PApplet) {
        val awtColor: Color? = JColorChooser.showDialog(p.frame, "Node Color", Color(DrawOptions.nodeFillColor))
        awtColor?.let { tintColor = awtColor.rgb }
    }

    private fun generatePorts(type: NodeType): List<Port> {
        val inputPorts = type.ports.filter { it.input }.withIndex().map { (idx, p) ->
            p.toPort(
                this,
                Vec2(0f, NodeDrawOptions.portStartY + idx.toFloat() * NodeDrawOptions.portSpacing)
            )
        }
        val outputPorts = type.ports.filter { !it.input }.withIndex().map { (idx, p) ->
            p.toPort(
                this,
                Vec2(width, NodeDrawOptions.portStartY + idx.toFloat() * NodeDrawOptions.portSpacing)
            )
        }
        return inputPorts + outputPorts
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
        // TODO respect titleTextSize
        p.text(customName, width / 2f, 0f)
        p.fill(DrawOptions.uiColor)
        p.text(type.nodeName, width / 2f, NodeDrawOptions.titleTextSize + 2f)

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

fun getIntrinsics(windowWidth: Float): List<Node> =
    listOf(
        IntrinsicNode(NodeType.FREQ, Vec2(20f, 20f)),
        IntrinsicNode(NodeType.GATE, Vec2(20f, 120f)),
        IntrinsicNode(NodeType.LCHAN, Vec2(windowWidth - NodeDrawOptions.width - 20f, 20f)),
        IntrinsicNode(NodeType.RCHAN, Vec2(windowWidth - NodeDrawOptions.width - 20f, 120f)),
    )