import processing.core.PApplet;

fun main(args: Array<String>) {
    PApplet.main("Main")
}

object DrawOptions {
    val bgColor = color("#102027")
    val uiColor = color("#bdbaac")
    val highlightOverlayColor = color("#ffffff22")
    val nodeFillColor = color("#37474f")
    val nodeTitleColor = color("#ffffff")
    val linkColor = color("#90a4ae96")
    val intrinsicTintColor = color("#ffffff")
    const val textSize = 14f
}

class Main : PApplet() {
    private var nodes = mutableListOf<Node>()
    private var links = mutableListOf<Link>()
    private val selectables: Iterable<SelectableObject> get() = nodes.flatMap { it.ports }.plus(nodes)
    private val drawables: Iterable<Drawable> get() = nodes.asIterable().plus(links.asIterable())
    private var selection: SelectableObject? = null
    private var linkStartedPort: Port? = null

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
        getSelection()
        drawables.forEach { it.draw(this) }

        linkStartedPort?.let {
            stroke(DrawOptions.linkColor)
            strokeWeight(3f)
            drawBezier(this, it.absoluteLocation, Vec2(mouseX.toFloat(), mouseY.toFloat()))
        }
    }

    override fun keyPressed() {
        when (key) {
            's' -> createNode(NodeType.SINOSC)
            'a' -> createNode(NodeType.ADSR)
            ' ' -> {
                if (selection != null && selection is Node) {
                    (selection as Node).mouseSnapped = !(selection as Node).mouseSnapped
                }
            }
            'c' -> connectOrCreateLink()
            'x' -> deleteNodeOrLink()
            'e' -> when (selection) {
                is Node -> (selection as Node).editCustomName(this)
            }
            'E' -> when (selection) {
                is Node -> (selection as Node).editTintColor(this)
            }
        }
    }

    private fun getSelection() {
        selectables.forEach { it.selected = false }
        selection = selectables.find { it.contains(Vec2(mouseX.toFloat(), mouseY.toFloat())) }
        selection?.selected = true
    }

    private fun createNode(type: NodeType) {
        if (selection is Node) {
            (selection as? Node)?.mouseSnapped = false
        }
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
            println("Link cannot connect to same node")
            return
        }
        if (a is InputPort == b is InputPort) {
            println("Link must connect an output to an input")
            return
        }
        val input: InputPort = if (a is InputPort) a else b as InputPort
        val output: OutputPort = if (a is OutputPort) a else b as OutputPort
        val newLink = Link(input, output)
        for (l in links) {
            if (l.outputPort == output && l.inputPort == input) {
                println("Attempt to create duplicate link")
                return
            }
        }

        // Only one link can go into a given input, remove the others
        links.removeAll { it.inputPort == input }
        links.add(newLink)
    }

    private fun deleteNodeOrLink() {
        when (selection) {
            is Port -> links.removeAll { it.inputPort == selection || it.outputPort == selection }
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