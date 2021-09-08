import processing.core.PApplet
import javax.swing.JOptionPane

object PortDrawOptions {
    const val ellipseSize = 25f
    const val portHeight = ellipseSize
    const val textInset = 15f
    const val selectedTextInset = 25f
    val outlineColor = DrawOptions.uiColor
}

abstract class Port(val parent: Node, val name: String, val location: Vec2) : SelectableObject {
    override var selected = false
    protected val inset: Float
        get() = if (selected) PortDrawOptions.selectedTextInset else PortDrawOptions.textInset

    override fun contains(p: Vec2): Boolean =
        p.distanceTo(absoluteLocation) <= PortDrawOptions.ellipseSize / 2f

    val absoluteLocation: Vec2 get() = parent.location + location

    override fun draw(p: PApplet) {
        p.stroke(PortDrawOptions.outlineColor)
        p.fill(DrawOptions.nodeFillColor)

        p.ellipseMode(PApplet.CENTER)
        p.ellipse(location.x, location.y, PortDrawOptions.ellipseSize, PortDrawOptions.ellipseSize)
        if (selected) {
            p.noStroke()
            p.fill(DrawOptions.highlightOverlayColor)
            p.ellipse(
                location.x, location.y, PortDrawOptions.ellipseSize, PortDrawOptions.ellipseSize
            )
        }
    }
}

class InputPort(
    parent: Node, name: String, location: Vec2, var multValue: Float, var biasValue: Float = 0f
) : Port(parent, name, location) {
    var hasConnectedLink = false

    override fun draw(p: PApplet) {
        super.draw(p)
        p.textAlign(PApplet.LEFT, PApplet.CENTER)
        p.fill(DrawOptions.uiColor)
        // TODO draw alternate if linked?
        var textX = location.x + inset
        p.text(name, textX, location.y)
        textX += p.textWidth(name) + 5f

        // multipliers and biases can only be added to non-intrinsics
        if (parent !is IntrinsicNode) {
            // only display multiplier if this port has a connected link (otherwise the mult will have no effect)
            if (hasConnectedLink) {
                p.fill(DrawOptions.portMultTextColor)
                val multString = "x%.4f".format(multValue).formatPrecision()
                p.text(multString, textX, location.y)
                textX += p.textWidth(multString) + 5f
            }
            // only display bias if it is not zero
            if (!hasConnectedLink || biasValue != 0f) {
                p.fill(DrawOptions.portBiasTextColor)
                val formatText = if (biasValue >= 0f) "+%.4f" else "%.4f"
                p.text(formatText.format(biasValue).formatPrecision(), textX, location.y)
            }
        }
    }

    fun editMultValue(p: PApplet) {
        if (parent !is IntrinsicNode) {
            val input: Float? =
                JOptionPane.showInputDialog(p.frame, "Enter mult value")?.toFloatOrNull()
            input?.let { multValue = input }
        }
    }

    fun editBiasValue(p: PApplet) {
        if (parent !is IntrinsicNode) {
            val input: Float? =
                JOptionPane.showInputDialog(p.frame, "Enter bias value")?.toFloatOrNull()
            input?.let { biasValue = input }
        }
    }

    // remove trailing zeros and period
    private fun String.formatPrecision(): String = this.trimEnd('0').trimEnd('.')
}

class OutputPort(parent: Node, name: String, location: Vec2) : Port(parent, name, location) {
    override fun draw(p: PApplet) {
        super.draw(p)
        p.textAlign(PApplet.RIGHT, PApplet.CENTER)
        p.fill(DrawOptions.uiColor)
        p.text(name, location.x - inset, location.y)
    }
}