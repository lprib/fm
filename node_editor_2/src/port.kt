import processing.core.PApplet

object PortDrawOptions {
    const val ellipseSize = 20f
    const val textInset = 15f
    const val selectedTextInset = 25f
    val outlineColor = DrawOptions.uiColor
}

abstract class Port(val parent: Node, val name: String, val location: Vec2) : SelectableObject {
    override var selected = false
    protected val inset: Float
        get() = if (selected) PortDrawOptions.selectedTextInset else PortDrawOptions.textInset

    override fun contains(p: Vec2): Boolean = p.distanceTo(absoluteLocation) <= PortDrawOptions.ellipseSize / 2f

    val absoluteLocation: Vec2 get() = parent.location + location

    override fun draw(p: PApplet) {
        p.stroke(PortDrawOptions.outlineColor)
        p.fill(DrawOptions.nodeFillColor)

        p.ellipseMode(PApplet.CENTER)
        p.ellipse(location.x, location.y, PortDrawOptions.ellipseSize, PortDrawOptions.ellipseSize)
        if (selected) {
            p.noStroke()
            p.fill(DrawOptions.highlightOverlayColor)
            p.ellipse(location.x, location.y, PortDrawOptions.ellipseSize, PortDrawOptions.ellipseSize)
        }
    }
}

class InputPort(parent: Node, name: String, location: Vec2, var value: Float) : Port(parent, name, location) {
    override fun draw(p: PApplet) {
        super.draw(p)
        p.textAlign(PApplet.LEFT, PApplet.CENTER)
        p.fill(DrawOptions.uiColor)
        // TODO draw alternate if linked?
        p.text(name, location.x + inset, location.y)
    }
}

class OutputPort(parent: Node, name: String, location: Vec2) : Port(parent, name, location) {
    override fun draw(p: PApplet) {
        super.draw(p)
        p.textAlign(PApplet.RIGHT, PApplet.CENTER)
        p.fill(DrawOptions.uiColor)
        p.text(name, location.x - inset, location.y)
    }
}