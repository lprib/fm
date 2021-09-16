package editor

import kotlinx.serialization.ExperimentalSerializationApi
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

@ExperimentalSerializationApi
class WebsocketConnection(val main: Main, host: String) : WebSocketClient(URI(host)) {
    override fun onOpen(handshake: ServerHandshake) {
        main.notify.send(main, "Connected to server")
    }

    override fun onMessage(message: String) {
        println("message $message")
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        main.notify.send(main, "Server connection closed (code $code)")
    }

    override fun onError(exception: Exception) {
        main.notify.send(main, "Websocket server error: ${exception.message}")
    }
}