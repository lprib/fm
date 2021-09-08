import processing.core.PApplet

data class Notification(val text: String, val creationMillis: Int)

class NotificationQueue : Drawable {
    val notifications = arrayListOf<Notification>()
    val notificationStayTime = 4000
    val padding = 10f
    val notificationHeight = DrawOptions.textSize + padding

    override fun draw(p: PApplet) {
        val currentTime = p.millis()
        notifications.removeAll { (currentTime - it.creationMillis) >= notificationStayTime }
        notifications.forEachIndexed { i, notification ->
            p.fill(DrawOptions.notificationColor)
            p.strokeWeight(1f)
            p.stroke(DrawOptions.uiColor)
            val x = 20f + i.toFloat() * (notificationHeight + 10f)
            p.rect(20f, x, p.textWidth(notifications[i].text) + padding, notificationHeight, 5f, 5f, 5f, 5f)
            p.fill(DrawOptions.uiColor)
            p.textAlign(PApplet.LEFT, PApplet.TOP)
            p.text(notifications[i].text, 20f + padding / 2f, x + padding / 2f)
        }
    }

    fun send(p: PApplet, text: String) {
        notifications.add(Notification(text, p.millis()))
    }
}