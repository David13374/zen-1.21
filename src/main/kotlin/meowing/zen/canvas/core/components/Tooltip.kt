package meowing.zen.canvas.core.components

import meowing.zen.canvas.core.CanvasElement
import meowing.zen.canvas.core.Pos
import meowing.zen.canvas.core.Size

class Tooltip(
    backgroundColor: Int = 0xFF1e1e1e.toInt(),
    borderColor: Int = 0xFF555759.toInt(),
    borderRadius: Float = 4f,
    borderThickness: Float = 1f,
    padding: FloatArray = floatArrayOf(4f, 4f, 4f, 4f),
    hoverColor: Int? = 0xFF1e1e1e.toInt(),
    pressedColor: Int? = 0xFF1e1e1e.toInt(),
    widthType: Size = Size.Auto,
    heightType: Size = Size.Auto
) : CanvasElement<Tooltip>(widthType, heightType) {
    private val backgroundRect = Rectangle(backgroundColor, borderColor, borderRadius, borderThickness, padding, hoverColor, pressedColor, widthType, heightType)
        .childOf(this)

    val innerText = Text("Tooltip", 0xFFFFFFFF.toInt(), 12f)
        .setPositioning(Pos.ParentCenter, Pos.ParentCenter)
        .childOf(backgroundRect)

    init {
        setSizing(Size.Auto, Size.Auto)
        setPositioning(0f, Pos.ParentCenter, -40f, Pos.ParentPixels)
        ignoreMouseEvents()
        setFloating()
        backgroundRect.visible = false
        innerText.visible = false
    }

    override fun onRender(mouseX: Float, mouseY: Float) {
        if (visible) {
            backgroundRect.visible = true
            backgroundRect.width = width
            backgroundRect.height = height
        } else {
            backgroundRect.visible = false
        }
    }
}