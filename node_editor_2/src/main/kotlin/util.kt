import processing.core.PApplet
import kotlin.math.pow
import kotlin.math.sqrt

interface Drawable {
    fun draw(p: PApplet)
}

interface SelectableObject : Drawable {
    var selected: Boolean
    fun contains(p: Vec2): Boolean
}

data class Vec2(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    fun distanceTo(other: Vec2): Float = sqrt((other.x - x).pow(2) + (other.y - y).pow(2))
}

fun color(s: String): Int {
    if (!s.startsWith("#")) {
        throw IllegalArgumentException("Color definition must start with '#'")
    }
    if (s.length == 7) {
        return s.substring(1..6).toInt(16) or (0xff shl 24)
    } else if (s.length == 9) {
        val alpha = s.substring(7..8).toInt(16)
        return s.substring(1..6).toInt(16) or (alpha shl 24)
    }
    throw IllegalArgumentException("Color must have either 7 or 9 characters")
}

fun drawBezier(p: PApplet, start: Vec2, end: Vec2) {
    val left = if (start.x < end.x) start else end
    val right = if (start.x >= end.x) start else end
    val controlInset = (right.x - left.x) / 2f
    val control1x = left.x + controlInset
    val control2x = right.x - controlInset
    p.noFill()
    p.bezier(left.x, left.y, control1x, left.y, control2x, right.y, right.x, right.y)
}