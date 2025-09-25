package meowing.zen.ui.components

import meowing.zen.Zen.Companion.mc
import net.minecraft.client.gui.screen.Screen
import org.lwjgl.glfw.GLFW
import xyz.meowing.vexel.utils.render.NVGRenderer
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

class TextInputComponent(
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var radius: Float,
    val accentColor: Color,
    val hoverColor: Color,
    val placeholder: String = ""
) {
    var value = ""
        set(newVal) {
            if (field == newVal) return
            field = newVal
            cursorIndex = cursorIndex.coerceIn(0, field.length)
            selectionAnchor = selectionAnchor.coerceIn(0, field.length)
        }

    val textPadding = 4

    var focused = false
    private var isDragging = false
    private var caretVisible = true
    private var isHovered = false

    private val normalBorderColor = Color(60, 60, 60)

    private var lastBlink = System.currentTimeMillis()
    private val caretBlinkRate = 500L

    private var cursorIndex = value.length
    private var selectionAnchor = value.length

    private val selectionStart: Int get() = min(cursorIndex, selectionAnchor)
    private val selectionEnd: Int get() = max(cursorIndex, selectionAnchor)
    private val hasSelection: Boolean get() = selectionStart != selectionEnd

    var scrollOffset = 0
    private var lastClickTime = 0L
    private var clickCount = 0

    fun draw(mouseX: Int, mouseY: Int) {
        isHovered = mouseX in x..(x + width) && mouseY in y..(y + height)

        val borderColor = when {
            focused -> accentColor
            isHovered -> hoverColor
            else -> normalBorderColor
        }

        NVGRenderer.rect((x - 1).toFloat(), (y - 1).toFloat(), (width + 2).toFloat(), (height + 2).toFloat(), borderColor.rgb, radius)
        NVGRenderer.rect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), Color(20, 20, 20).rgb, radius)

        val shouldShowPlaceholder = value.isEmpty() && !focused
        val textToRender = if (shouldShowPlaceholder) placeholder else value
        val textColor = if (shouldShowPlaceholder) Color(120, 120, 120).rgb else Color.WHITE.rgb
        val textSize = 12f
        val textY = y + (height - textSize) / 2

        NVGRenderer.pushScissor(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

        if (hasSelection && !shouldShowPlaceholder) {
            val selStartStr = value.substring(0, selectionStart)
            val selEndStr = value.substring(0, selectionEnd)
            val x1 = x + textPadding - scrollOffset + NVGRenderer.textWidth(selStartStr, textSize, NVGRenderer.defaultFont)
            val x2 = x + textPadding - scrollOffset + NVGRenderer.textWidth(selEndStr, textSize, NVGRenderer.defaultFont)
            NVGRenderer.rect(x1, textY, x2 - x1, textSize, accentColor.rgb)
        }

        NVGRenderer.text(textToRender, (x + textPadding - scrollOffset).toFloat(), textY, textSize, textColor, NVGRenderer.defaultFont)

        if (focused && caretVisible && !shouldShowPlaceholder) {
            val textBeforeCaret = value.take(cursorIndex)
            val caretXPos = x + textPadding - scrollOffset + NVGRenderer.textWidth(textBeforeCaret, textSize, NVGRenderer.defaultFont)
            if (caretXPos >= x + textPadding - 1 && caretXPos <= x + textPadding + width - textPadding * 2) {
                NVGRenderer.rect(caretXPos, textY, 1f, textSize, Color.WHITE.rgb)
            }
        }

        NVGRenderer.popScissor()

        if (System.currentTimeMillis() - lastBlink > caretBlinkRate) {
            caretVisible = !caretVisible
            lastBlink = System.currentTimeMillis()
        }
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return false

        val clickedOnField = mouseX in x..(x + width) && mouseY in y..(y + height)

        if (clickedOnField) {
            focused = true
            isDragging = true

            val clickRelX = mouseX - (x + textPadding - scrollOffset)
            val newCursorIndex = getCharIndexAtAbsX(clickRelX.toFloat())

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 250) clickCount++
            else clickCount = 1

            lastClickTime = currentTime

            when (clickCount) {
                1 -> {
                    cursorIndex = newCursorIndex
                    if (!Screen.hasShiftDown()) {
                        selectionAnchor = cursorIndex
                    }
                }
                2 -> selectWordAt(newCursorIndex)
                else -> {
                    selectAll()
                    clickCount = 0
                }
            }
            resetCaretBlink()
            return true
        } else {
            focused = false
            isDragging = false
            return false
        }
    }

    fun keyTyped(keyCode: Int, charTyped: Char): Boolean {
        if (!focused) return false

        val ctrlDown = Screen.hasControlDown()
        val shiftDown = Screen.hasShiftDown()

        when (keyCode) {
            GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER -> {
                focused = false
                return true
            }
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (ctrlDown) deletePrevWord()
                else deleteChar(-1)
                return true
            }
            GLFW.GLFW_KEY_DELETE -> {
                if (ctrlDown) deleteNextWord()
                else deleteChar(1)
                return true
            }
            GLFW.GLFW_KEY_LEFT -> {
                if (ctrlDown) moveWord(-1, shiftDown)
                else moveCaret(-1, shiftDown)
                return true
            }
            GLFW.GLFW_KEY_RIGHT -> {
                if (ctrlDown) moveWord(1, shiftDown)
                else moveCaret(1, shiftDown)
                return true
            }
            GLFW.GLFW_KEY_HOME -> {
                moveCaretTo(0, shiftDown)
                return true
            }
            GLFW.GLFW_KEY_END -> {
                moveCaretTo(value.length, shiftDown)
                return true
            }
        }

        if (ctrlDown) {
            when (charTyped) {
                'a' -> { selectAll(); return true }
                'c' -> { copySelection(); return true }
                'v' -> { paste(); return true }
                'x' -> { cutSelection(); return true }
            }
        }
        return false
    }

    fun charTyped(chr: Char): Boolean {
        if (!focused || chr.code < 32 || chr == 127.toChar()) return false
        insertText(chr.toString())
        return true
    }

    private fun resetCaretBlink() {
        lastBlink = System.currentTimeMillis()
        caretVisible = true
    }

    private fun getCharIndexAtAbsX(absClickX: Float): Int {
        if (absClickX <= 0) return 0
        var currentWidth = 0f
        for (i in value.indices) {
            val charWidth = NVGRenderer.textWidth(value[i].toString(), 12f, NVGRenderer.defaultFont)
            if (absClickX < currentWidth + charWidth / 2) {
                return i
            }
            currentWidth += charWidth
        }
        return value.length
    }

    private fun selectWordAt(pos: Int) {
        if (value.isEmpty()) return
        val currentPos = pos.coerceIn(0, value.length)

        if (currentPos < value.length && !value[currentPos].isWhitespace()) {
            var start = currentPos
            while (start > 0 && !value[start - 1].isWhitespace()) start--
            var end = currentPos
            while (end < value.length && !value[end].isWhitespace()) end++
            cursorIndex = end
            selectionAnchor = start
        } else {
            cursorIndex = currentPos
            selectionAnchor = currentPos
        }
        ensureCaretVisible()
    }

    private fun insertText(text: String) {
        val builder = StringBuilder(value)
        val newCursorPos = if (!hasSelection) cursorIndex
        else {
            val currentSelectionStart = selectionStart
            builder.delete(currentSelectionStart, selectionEnd)
            currentSelectionStart
        }

        builder.insert(newCursorPos, text)
        this.value = builder.toString()
        cursorIndex = (newCursorPos + text.length).coerceIn(0, this.value.length)
        selectionAnchor = cursorIndex

        ensureCaretVisible()
        resetCaretBlink()
    }

    private fun deleteChar(direction: Int) {
        var textChanged = false
        var newText = value
        var newCursor = cursorIndex

        if (hasSelection) {
            val builder = StringBuilder(value)
            val selStart = selectionStart
            builder.delete(selStart, selectionEnd)
            newText = builder.toString()
            newCursor = selStart
            textChanged = true
        } else {
            if (direction == -1 && cursorIndex > 0) {
                val builder = StringBuilder(value)
                builder.deleteCharAt(cursorIndex - 1)
                newText = builder.toString()
                newCursor = cursorIndex - 1
                textChanged = true
            } else if (direction == 1 && cursorIndex < value.length) {
                val builder = StringBuilder(value)
                builder.deleteCharAt(cursorIndex)
                newText = builder.toString()
                textChanged = true
            }
        }

        if (textChanged) {
            this.value = newText
            cursorIndex = newCursor.coerceIn(0, this.value.length)
            selectionAnchor = cursorIndex

            val maxScroll = max(0, NVGRenderer.textWidth(this.value, 12f, NVGRenderer.defaultFont).toInt() - (width - textPadding * 2))
            if (scrollOffset > maxScroll) {
                scrollOffset = maxScroll
            }

            ensureCaretVisible()
        }
        resetCaretBlink()
    }

    private fun moveCaret(amount: Int, shiftHeld: Boolean) {
        cursorIndex = (cursorIndex + amount).coerceIn(0, value.length)
        if (!shiftHeld) {
            selectionAnchor = cursorIndex
        }
        ensureCaretVisible()
        resetCaretBlink()
    }

    private fun moveCaretTo(position: Int, shiftHeld: Boolean) {
        cursorIndex = position.coerceIn(0, value.length)
        if (!shiftHeld) {
            selectionAnchor = cursorIndex
        }
        ensureCaretVisible()
        resetCaretBlink()
    }

    private fun moveWord(direction: Int, shiftHeld: Boolean) {
        cursorIndex = findWordBoundary(cursorIndex, direction)
        if (!shiftHeld) {
            selectionAnchor = cursorIndex
        }
        ensureCaretVisible()
        resetCaretBlink()
    }

    private fun findWordBoundary(startIndex: Int, direction: Int): Int {
        var i = startIndex
        val len = value.length
        if (direction < 0) {
            if (i > 0) i--
            while (i > 0 && value[i].isWhitespace()) i--
            while (i > 0 && !value[i - 1].isWhitespace()) i--
        } else {
            while (i < len && !value[i].isWhitespace()) i++
            while (i < len && value[i].isWhitespace()) i++
        }
        return i.coerceIn(0, len)
    }

    private fun deletePrevWord() {
        if (hasSelection) {
            deleteChar(0)
            return
        }
        if (cursorIndex == 0) return
        val oldCursor = cursorIndex
        cursorIndex = findWordBoundary(cursorIndex, -1)
        selectionAnchor = oldCursor
        deleteChar(0)
    }

    private fun deleteNextWord() {
        if (hasSelection) {
            deleteChar(0)
            return
        }
        if (cursorIndex == value.length) return
        val oldCursor = cursorIndex
        cursorIndex = findWordBoundary(cursorIndex, 1)
        selectionAnchor = oldCursor
        deleteChar(0)
    }

    private fun selectAll() {
        selectionAnchor = 0
        cursorIndex = value.length
        resetCaretBlink()
    }

    private fun getSelectedText(): String {
        return if (hasSelection) value.substring(selectionStart, selectionEnd) else ""
    }

    private fun copySelection() {
        if (!hasSelection) return
        mc.keyboard.clipboard = getSelectedText()
    }

    private fun cutSelection() {
        if (!hasSelection) return
        copySelection()
        deleteChar(0)
    }

    private fun paste() {
        val clipboardText = mc.keyboard.clipboard
        if (clipboardText.isNotEmpty()) {
            insertText(clipboardText)
        }
    }

    private fun ensureCaretVisible() {
        val caretXAbsolute = NVGRenderer.textWidth(value.substring(0, cursorIndex.coerceIn(0, value.length)), 12f, NVGRenderer.defaultFont).toInt()
        val visibleTextStart = scrollOffset
        val visibleTextEnd = scrollOffset + (width - textPadding * 2)

        if (caretXAbsolute < visibleTextStart) {
            scrollOffset = caretXAbsolute
        } else if (caretXAbsolute > visibleTextEnd - 1) {
            scrollOffset = caretXAbsolute - (width - textPadding * 2) + 1
        }

        val maxScrollPossible = max(0, NVGRenderer.textWidth(value, 12f, NVGRenderer.defaultFont).toInt() - (width - textPadding * 2))
        scrollOffset = scrollOffset.coerceIn(0, maxScrollPossible)
        if (NVGRenderer.textWidth(value, 12f, NVGRenderer.defaultFont).toInt() <= width - textPadding * 2) {
            scrollOffset = 0
        }
    }
}