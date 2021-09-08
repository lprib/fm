import kotlinx.serialization.ExperimentalSerializationApi
import processing.core.PApplet
import javax.swing.UIManager

@ExperimentalSerializationApi
fun main() {
    try {
        UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel")
    } catch (e: Exception) {
        // ignore
    }
    PApplet.main("Main")
}

val patch1 = """
{ "nodes":[{ "type":"adsr", "_x":227.0, "_y":397.5, "_name":"", "_color":-13154481, "inputPorts":{ "gate":{ "mult":1.0, "bias":0.0, "link":0 }, "a":{ "mult":1.0, "bias":0.0 }, "d":{ "mult":1.0, "bias":0.0 }, "s":{ "mult":1.0, "bias":1.0 }, "r":{ "mult":1.0, "bias":0.0 } }, "outputPorts":{ "out":{ "link":1 } } },{ "type":"sinosc", "_x":491.0, "_y":235.5, "_name":"", "_color":-13154481, "inputPorts":{ "freq":{ "mult":1.0, "bias":0.0, "link":2 }, "phase":{ "mult":1.0, "bias":0.0 }, "vol":{ "mult":1.0, "bias":0.0, "link":1 }, "feedback":{ "mult":1.0, "bias":0.0 } }, "outputPorts":{ "out":{ "link":3 } } },{ "type":"sinosc", "_x":868.0, "_y":157.5, "_name":"", "_color":-13154481, "inputPorts":{ "freq":{ "mult":1.0, "bias":0.0, "link":2 }, "phase":{ "mult":1.0, "bias":0.0, "link":3 }, "vol":{ "mult":1.0, "bias":0.0, "link":1 }, "feedback":{ "mult":1.0, "bias":0.0 } }, "outputPorts":{ "out":{ "link":4 } } }], "io":{ "freq":2, "gate":0, "lchan":4, "rchan":4 } }
"""

object DrawOptions {
    val bgColor = color("#102027")
    val uiColor = color("#bdbaac")
    val portMultTextColor = color("#bd8080")
    val portBiasTextColor = color("#807dba")
    val highlightOverlayColor = color("#ffffff22")
    val nodeFillColor = color("#37474f")
    val nodeTitleColor = color("#ffffff")
    val linkColor = color("#90a4ae96")
    val intrinsicTintColor = color("#003c96")
    val notificationColor = color("#7f0000")
    const val textSize = 14f
}

@ExperimentalSerializationApi
class Main : PApplet() {
    private var nodes = mutableListOf<Node>()
    private var links = mutableListOf<Link>()
    private val selectables: Iterable<SelectableObject> get() = nodes.flatMap { it.ports }.plus(nodes)
    private val drawables: Iterable<Drawable> get() = nodes.asIterable().plus(links.asIterable())
    private var selection: SelectableObject? = null
    private var linkStartedPort: Port? = null
    private val notify = NotificationQueue()

    override fun settings() {
        size(1400, 800)
        smooth()
    }

    override fun setup() {
        textSize(DrawOptions.textSize)
        println(DrawOptions.linkColor)
        nodes.addAll(getIntrinsics(width.toFloat()))
    }

    override fun draw() {
        background(DrawOptions.bgColor)
        findSelection()
        drawables.forEach { it.draw(this) }

        linkStartedPort?.let {
            stroke(DrawOptions.linkColor)
            strokeWeight(3f)
            drawBezier(this, it.absoluteLocation, Vec2(mouseX.toFloat(), mouseY.toFloat()))
        }
        notify.draw(this)
    }

    override fun keyPressed() {
        when (key) {
            's' -> createNode(NodeType.SINOSC)
            'a' -> createNode(NodeType.ADSR)
            'A' -> createNode(NodeType.MIXER)
            ' ' -> {
                if (selection != null && selection is Node) {
                    (selection as Node).mouseSnapped = !(selection as Node).mouseSnapped
                }
            }
            'c' -> connectOrCreateLink()
            'd' -> deleteNodeOrLink()
            'e' -> when (selection) {
                is InputPort -> (selection as InputPort).editMultValue(this)
                is Node -> (selection as Node).editCustomName(this)
            }
            'E' -> when (selection) {
                is InputPort -> (selection as InputPort).editBiasValue(this)
                is Node -> (selection as Node).editTintColor(this)
            }
            'p' -> println(serde.serializePatch(nodes, links))
            'l' -> {
                val (anodes, alinks) = serde.deserializePatch(patch1, width.toFloat())
                nodes = anodes
                links = alinks
            }
        }
    }

    private fun findSelection() {
        selectables.forEach { it.selected = false }
        selection = selectables.find { it.contains(Vec2(mouseX.toFloat(), mouseY.toFloat())) }
        selection?.selected = true
    }

    private fun createNode(type: NodeType) {
        nodes.forEach { it.selected = false }
        val new = Node(type)
        new.mouseSnapped = true
        nodes.add(new)
    }

    private fun connectOrCreateLink() {
        if (linkStartedPort == null) {
            // Create new link start
            if (selection != null && selection is Port) {
                linkStartedPort = selection as Port
            }
        } else {
            // try finish link
            val a = linkStartedPort
            val b = selection as? Port
            if (a != null && b != null) {
                tryCreateLink(a, b)
            }
            linkStartedPort = null
        }
    }

    private fun tryCreateLink(a: Port, b: Port) {
        if (a.parent == b.parent) {
            notify.send(this, "Link cannot connect to same node")
            return
        }
        if (a is InputPort == b is InputPort) {
            notify.send(this, "Link must connect an output to an input")
            return
        }
        val input: InputPort = if (a is InputPort) a else b as InputPort
        val output: OutputPort = if (a is OutputPort) a else b as OutputPort
        val newLink = Link(input, output)
        for (l in links) {
            if (l.outputPort == output && l.inputPort == input) {
                notify.send(this, "Attempt to create duplicate link")
                return
            }
        }

        // Only one link can go into a given input, remove the others
        links.removeAll { it.inputPort == input }
        links.add(newLink)
    }

    private fun deleteNodeOrLink() {
        when (selection) {
            is Port -> links.removeAll {
                if (it.inputPort == selection || it.outputPort == selection) {
                    it.notifyDelete()
                    true
                } else {
                    false
                }
            }
            is IntrinsicNode -> {
                // cannot remove intrinsics
            }
            is Node -> {
                links.removeAll { it.inputPort.parent == selection || it.outputPort.parent == selection }
                nodes.remove(selection)
            }
        }
    }
}