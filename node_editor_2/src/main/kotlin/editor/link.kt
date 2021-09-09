package editor

import processing.core.PApplet

object LinkDrawOptions {
    const val ellipseSize = 10f
}

class Link(val inputPort: InputPort, val outputPort: OutputPort) : Drawable {
    init {
        inputPort.hasConnectedLink = true
    }

    override fun draw(p: PApplet) {
        p.stroke(DrawOptions.linkColor)
        p.strokeWeight(3f)
        drawBezier(p, inputPort.absoluteLocation, outputPort.absoluteLocation)
        p.noStroke()
        // ellipses should be link color without transparency
        p.fill(DrawOptions.linkColor or (0xFF shl 24))
        p.ellipse(
            inputPort.absoluteLocation.x,
            inputPort.absoluteLocation.y,
            LinkDrawOptions.ellipseSize,
            LinkDrawOptions.ellipseSize
        )
        p.ellipse(
            outputPort.absoluteLocation.x,
            outputPort.absoluteLocation.y,
            LinkDrawOptions.ellipseSize,
            LinkDrawOptions.ellipseSize
        )
    }

    /**
     * On delete, notify the connected ports that they are no longer linked.
     * This MUST be called before deleting a link
     */
    fun notifyDelete() {
        inputPort.hasConnectedLink = false
    }
}