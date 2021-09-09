package editor

import processing.core.PApplet

object NotificationOptions {
    /**
     * Time in milliseconds that notifications will stay on the screen
     */
    const val notificationStayTime = 4000

    /**
     * Padding (left/right total and top/bottom total) that will be applied to text inside notification bubbles.
     */
    const val padding = 10f
}

data class Notification(val text: String, val creationMillis: Int)

class NotificationQueue : Drawable {
    private val notifications = arrayListOf<Notification>()

    override fun draw(p: PApplet) {
        val currentTime = p.millis()
        notifications.removeAll { (currentTime - it.creationMillis) >= NotificationOptions.notificationStayTime }
        notifications.forEachIndexed { i, notification ->
            p.fill(DrawOptions.notificationColor)
            p.strokeWeight(1f)
            p.stroke(DrawOptions.uiColor)
            val height = DrawOptions.textSize + NotificationOptions.padding
            val x = 20f + i.toFloat() * (height + 10f)
            p.rect(
                20f,
                x,
                p.textWidth(notifications[i].text) + NotificationOptions.padding,
                height,
                5f,
                5f,
                5f,
                5f
            )
            p.fill(DrawOptions.uiColor)
            p.textAlign(PApplet.LEFT, PApplet.TOP)
            p.text(
                notifications[i].text,
                20f + NotificationOptions.padding / 2f,
                x + NotificationOptions.padding / 2f
            )
        }
    }

    /**
     * Add a new notification to the queue, with message [text]
     */
    fun send(p: PApplet, text: String) {
        notifications.add(Notification(text, p.millis()))
    }
}