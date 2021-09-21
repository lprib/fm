package editor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import processing.core.PApplet
import serde.ClientRequest
import serde.deserializePatch
import serde.serializePatch
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager

@ExperimentalSerializationApi
fun main() {
    try {
        System.setProperty("awt.useSystemAAFontSettings", "on")
        System.setProperty("swing.aatext", "true")
        UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel")
    } catch (e: Exception) {
        // ignore
    }
    PApplet.main("editor.Main")
}

object DrawOptions {
    val bgColor = color("#102027")
    val uiColor = color("#bdbaac")
    val portMultTextColor = color("#bd8080")
    val portBiasTextColor = color("#807dba")
    val highlightOverlayColor = color("#ffffff22")
    val nodeFillColor = color("#37474f")
    val nodeTitleColor = color("#ffffff")
    val linkColor = color("#90a4ae90")
    val defaultTintColor = color("#d11e02")
    val intrinsicTintColor = color("#1a60c9")
    val notificationColor = color("#7f0000BB")
    const val textSize = 14f
}

val helpText = """
        s: create sinosc
        a: create adsr
        A: create mixer
        c: create or finish link
        d: delete node or links
        e: (node) edit name, (port) edit bias value
        E: (node) edit tint color, (port) edit mult value
        f: load patch from file
        F: save patch to file
        p: send current patch to websocket server
        P: reconnect with websocket server
        h: display this help
    """.trimIndent()

@ExperimentalSerializationApi
class Main : PApplet() {
    private var nodes = mutableListOf<Node>()
    private var links = mutableListOf<Link>()
    private val selectables: Iterable<SelectableObject>
        get() = nodes.flatMap { it.ports }.plus(nodes)
    private val drawables: Iterable<Drawable>
        get() = nodes.asIterable().plus(links.asIterable())
    private var selection: SelectableObject? = null
    private var linkStartedPort: Port? = null
    val notify = NotificationQueue()
    private val choose = JFileChooser()

    private val serverConnection = WebsocketConnection(this, "ws://localhost:8080")

    override fun settings() {
        size(1400, 800, P2D)
        smooth()
    }

    override fun setup() {
        textSize(DrawOptions.textSize)
        nodes.addAll(getIntrinsics(width.toFloat()))
        val currentPath = System.getProperty("user.dir")
        choose.currentDirectory = File(currentPath)

        try {
            serverConnection.connect()
        } catch (e: Exception) {
            notify.send(this, "Could not connect to server")
        }
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
                is InputPort -> (selection as InputPort).editBiasValue(this)
                is Node -> (selection as Node).editCustomName(this)
            }
            'E' -> when (selection) {
                is InputPort -> (selection as InputPort).editMultValue(this)
                is Node -> (selection as Node).editTintColor(this)
            }
            'f' -> loadPatch()
            'F' -> savePatch()
            'h' -> notify.send(this, helpText, 10000)
            'p' -> {
                val req = ClientRequest(serializePatch(nodes, links))
                val reqString = Json.encodeToString(req)
                try {
                    serverConnection.send(reqString)
                } catch (e: Exception) {
                    notify.send(this, "Error sending patch to server")
                }
            }
            'P' -> reconnectToServer()
            ESC -> key = 0.toChar()
        }
    }

    /**
     * Iterate over all selectables, and find the first one that is under the cursor.
     * Mark this object as selected and all others as not selected.
     */
    private fun findSelection() {
        selectables.forEach { it.selected = false }
        selection = selectables.find { it.contains(Vec2(mouseX.toFloat(), mouseY.toFloat())) }
        selection?.selected = true
    }

    /**
     * Create node of [type]. Unsnap all nodes, and snap the new node to the mouse.
     */
    private fun createNode(type: NodeType) {
        nodes.forEach { it.mouseSnapped = false }
        val new = Node(type)
        new.mouseSnapped = true
        nodes.add(new)
    }

    /**
     * If a link is already started but not completed, try to complete the link to the
     * selected port if any, see [tryCreateLink].
     * If a link has not been started, start a link from the selected port if any
     */
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

    /**
     * Attempt to create a link from [a] to [b]. [a] and [b] can be arbitrary input or output ports.
     * If the link cannot be created, posts a notification.
     */
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

    /**
     * Attempt to delete the selected object, be it link or node.
     * If a port is selected, all the links to that port will be deleted.
     */
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

    private fun loadPatch() {
        val ret = choose.showOpenDialog(frame)
        if (ret == JFileChooser.APPROVE_OPTION) {
            notify.send(this, "Loading patch ${choose.selectedFile.name}")
            val str = choose.selectedFile.bufferedReader().use { it.readText() }
            try {
                val (newNodes, newLinks) = deserializePatch(str, width.toFloat())
                nodes = newNodes
                links = newLinks
            } catch (e: Exception) {
                notify.send(this, "Error loading patch: ${e.message}", 7000)
            }
        }
    }

    private fun savePatch() {
        val ret = choose.showSaveDialog(frame)
        if (ret == JFileChooser.APPROVE_OPTION) {
            notify.send(this, "Saving patch to ${choose.selectedFile.name}")
            choose.selectedFile.bufferedWriter().use {
                it.write(Json.encodeToString(serializePatch(nodes, links)))
            }
        }
    }

    private fun reconnectToServer() {
        try {
            serverConnection.reconnect()
        } catch (e: Exception) {
            notify.send(this, "Could not reconnect to server")
        }
    }
}