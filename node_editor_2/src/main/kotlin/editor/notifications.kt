package editor

import processing.core.PApplet

object NotificationOptions {
    /**
     * Default time in milliseconds that notifications will stay on the screen
     */
    const val notificationStayTime = 4000

    /**
     * Padding (left/right total and top/bottom total) that will be applied to text inside notification bubbles.
     */
    const val padding = 10f
}

data class Notification(
    val text: String,
    val creationMillis: Int,
    val stayTime: Int = NotificationOptions.notificationStayTime
)

class NotificationQueue : Drawable {
    private val notifications = arrayListOf<Notification>()

    override fun draw(p: PApplet) {
        val currentTime = p.millis()
        notifications.removeAll { (currentTime - it.creationMillis) >= it.stayTime }
        var x = 10f
        notifications.forEach {
            p.fill(DrawOptions.notificationColor)
            p.strokeWeight(1f)
            p.stroke(DrawOptions.uiColor)
            val lineCount = it.text.count { char -> char == '\n' } + 1
            val height =
                (DrawOptions.textSize + NotificationOptions.padding / 2f) * lineCount + NotificationOptions.padding
            p.rect(
                20f, x, p.textWidth(it.text) + NotificationOptions.padding, height, 5f, 5f, 5f, 5f
            )
            p.fill(DrawOptions.uiColor)
            p.textAlign(PApplet.LEFT, PApplet.TOP)
            it.text.lines().forEachIndexed { i, line ->
                p.text(
                    line,
                    20f + NotificationOptions.padding / 2f,
                    x + NotificationOptions.padding / 2f + i * (DrawOptions.textSize + NotificationOptions.padding / 2f)
                )
            }
            x += height + NotificationOptions.padding
        }
    }

    /**
     * Add a new notification to the queue, with message [text].
     * Notification will stay on the screen for [stayTime] ms.
     */
    fun send(p: PApplet, text: String, stayTime: Int = NotificationOptions.notificationStayTime) {
        notifications.add(Notification(text, p.millis(), stayTime))
    }
}