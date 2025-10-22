package xyz.meowing.zen.features.general

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.CramSiblingConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.universal.UKeyboard
import xyz.meowing.zen.Zen
import xyz.meowing.zen.config.ui.constraint.ChildHeightConstraint
import xyz.meowing.zen.config.ui.types.ElementType
import xyz.meowing.zen.events.KeyEvent
import xyz.meowing.zen.events.MouseEvent
import xyz.meowing.zen.features.Feature
import xyz.meowing.zen.utils.DataUtils
import xyz.meowing.zen.utils.TickUtils
import xyz.meowing.zen.utils.Utils.createBlock
import org.lwjgl.glfw.GLFW
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.command.Commodore
import xyz.meowing.knit.api.input.KnitInputs
import xyz.meowing.knit.api.text.KnitText
import xyz.meowing.knit.api.text.core.ClickEvent
import xyz.meowing.zen.Zen.Companion.prefix
import xyz.meowing.zen.config.ConfigElement
import xyz.meowing.zen.config.ConfigManager
import java.awt.Color

data class KeybindEntry(
    var keys: MutableList<Int>,
    var command: String
)

data class KeybindData(val bindings: MutableList<KeybindEntry> = mutableListOf())

@Zen.Module
object KeyShortcuts : Feature("keyshortcuts") {
    val bindings get() = dataUtils.getData().bindings
    val dataUtils = DataUtils("keybind", KeybindData())
    private val pressedKeys = mutableSetOf<Int>()
    private val pressedMouseButtons = mutableSetOf<Int>()
    private val triggeredBindings = mutableSetOf<KeybindEntry>()

    override fun addConfig() {
        ConfigManager
            .addFeature("Key Shortcuts", "", "General", ConfigElement(
                "keyshortcuts",
                ElementType.Switch(false)
            ))
            .addFeatureOption("Open Keybind Manager", "Open Keybind Manager", "GUI", ConfigElement(
                "keyshortcutsgui",
                ElementType.Button("Open Manager") {
                    TickUtils.schedule(2) {
                        client.setScreen(KeybindGui())
                    }
                }
            ))
    }

    override fun initialize() {
        register<KeyEvent.Press> { event ->
            if (client.currentScreen != null) return@register

            if (event.keyCode > 0) {
                pressedKeys.add(event.keyCode)
                checkKeybindMatch()
            }
        }

        register<KeyEvent.Release> { event ->
            if (event.keyCode > 0) {
                pressedKeys.remove(event.keyCode)
                resetTriggeredBindings()
            }
        }

        register<MouseEvent.Click> { event ->
            if (client.currentScreen != null) return@register

            val mouseCode = -(event.button + 1)
            pressedMouseButtons.add(mouseCode)
            checkKeybindMatch()
        }

        register<MouseEvent.Release> { event ->
            pressedMouseButtons.remove(-(event.button + 1))
            resetTriggeredBindings()
        }
    }

    private fun checkKeybindMatch() {
        val allPressed = pressedKeys + pressedMouseButtons

        bindings.forEach { binding ->
            if (binding.keys.isEmpty()) return@forEach
            val isMatch = binding.keys.all { it in allPressed }

            if (isMatch && binding !in triggeredBindings) {
                triggeredBindings.add(binding)
                val command = binding.command.takeIf { it.isNotEmpty() } ?: return
                if (command.startsWith("/")) KnitChat.sendCommand(command) else KnitChat.sendMessage(command)
            }
        }
    }

    private fun resetTriggeredBindings() {
        val allPressed = pressedKeys + pressedMouseButtons
        triggeredBindings.removeIf { binding ->
            !binding.keys.all { it in allPressed }
        }
    }

    fun addBinding(keys: List<Int>, command: String): Boolean {
        if (command.isBlank() || keys.isEmpty() || bindings.any { it.keys == keys }) return false
        bindings.add(KeybindEntry(keys.toMutableList(), command))
        return true
    }

    fun removeBinding(index: Int): Boolean {
        if (index < 0 || index >= bindings.size) return false
        bindings.removeAt(index)
        return true
    }

    fun updateBinding(index: Int, keys: List<Int>, command: String): Boolean {
        if (index < 0 || index >= bindings.size || command.isBlank() || keys.isEmpty()) return false
        if (bindings.any { it.keys == keys && bindings.indexOf(it) != index }) return false
        bindings[index] = KeybindEntry(keys.toMutableList(), command)
        return true
    }
}

@Zen.Command
object KeybindCommand : Commodore("keybind", "zenkeybind", "zenkb") {
    init {
        runs {
            if (!KeyShortcuts.isEnabled()) {
                val message = KnitText
                    .literal("$prefix §cYou do not have the feature §bKeyShortcuts §cenabled!")
                    .onHover("Click to enable feature.")
                    .onClick(ClickEvent.RunCommand("/zen updateConfig keyshortcuts true false"))
                    .toVanilla()

                KnitChat.fakeMessage(message)
                return@runs
            }

            TickUtils.schedule(2) {
                client.setScreen(KeybindGui())
            }
        }
    }
}

class KeybindGui : WindowScreen(ElementaVersion.V2, newGuiScale = 2) {
    class KeybindText(
        initialValue: String = "",
        placeholder: String = "",
        var onChange: ((String) -> Unit)? = null
    ) : UIContainer() {
        var text: String = initialValue
        val input: UITextInput
        private val placeholderText: UIText?

        init {
            val container = createBlock(3f).constrain {
                x = 1.pixels()
                y = 1.pixels()
                width = 100.percent() - 2.pixels()
                height = 100.percent() - 2.pixels()
            }.setColor(Color(18, 24, 28, 255)) childOf this

            input = (UITextInput(text).constrain {
                x = 8.pixels()
                y = CenterConstraint()
                width = 100.percent() - 16.pixels()
                height = 10.pixels()
            }.setColor(Color(170, 230, 240, 255)) childOf container) as UITextInput

            placeholderText = (if (placeholder.isNotEmpty()) {
                UIText(placeholder).constrain {
                    x = 8.pixels()
                    y = CenterConstraint()
                }.setColor(Color(80, 120, 140, 255)) childOf container
            } else null) as UIText?

            updatePlaceholderVisibility()
            setupEventHandlers()
        }

        private fun setupEventHandlers() {
            onMouseClick {
                input.setText(text)
                input.grabWindowFocus()
            }

            input.onKeyType { _, _ ->
                text = input.getText()
                updatePlaceholderVisibility()
                onChange?.invoke(text)
            }

            input.onFocusLost {
                text = input.getText()
                onChange?.invoke(text)
            }
        }

        private fun updatePlaceholderVisibility() {
            placeholderText?.let { placeholder ->
                if (text.isEmpty()) placeholder.unhide(true)
                else placeholder.hide(true)
            }
        }
    }

    class KeybindButton(
        var keys: MutableList<Int> = mutableListOf(),
        var onKeysChanged: ((List<Int>) -> Unit)? = null
    ) : UIContainer() {
        var listening = false
        val button: UIComponent
        val keyText: UIText
        private val recordedKeys = mutableSetOf<Int>()

        init {
            button = createBlock(3f).constrain {
                width = 100.percent()
                height = 100.percent()
            }.setColor(Color(18, 24, 28, 255)) childOf this

            keyText = (UIText(getKeysName(keys)).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                textScale = 0.8.pixels()
            }.setColor(Color(170, 230, 240, 255)) childOf button) as UIText

            setupEventHandlers()
        }

        private fun setupEventHandlers() {
            onMouseClick { event ->
                if (!listening) {
                    startListening()
                } else {
                    val mouseCode = -(event.mouseButton + 1)
                    recordedKeys.add(mouseCode)
                    keyText.setText(getKeysName(recordedKeys.toList()))
                }
            }

            onMouseRelease {
                if (listening && recordedKeys.isNotEmpty()) stopListening()
            }
        }

        fun startListening() {
            listening = true
            recordedKeys.clear()
            keyText.setText("Press keys/buttons...")
            button.setColor(Color(40, 60, 80, 255))
        }

        fun stopListening() {
            listening = false
            keys = recordedKeys.toMutableList()
            keyText.setText(getKeysName(keys))
            button.setColor(Color(18, 24, 28, 255))
            onKeysChanged?.invoke(keys)
        }

        fun handleKeyPress(keyCode: Int) {
            if (listening && keyCode > 0) {
                recordedKeys.add(keyCode)
                keyText.setText(getKeysName(recordedKeys.toList()))
            }
        }

        fun handleKeyRelease(keyCode: Int) {
            if (listening && recordedKeys.isNotEmpty() && keyCode > 0) {
                stopListening()
            }
        }

        private fun getKeysName(keyList: List<Int>): String {
            if (keyList.isEmpty()) return "None"
            return keyList.joinToString(" + ") { getKeyName(it) }
        }
    }

    private val theme = object {
        val bg = Color(8, 12, 16, 255)
        val element = Color(12, 16, 20, 255)
        val accent = Color(100, 245, 255, 255)
        val accent2 = Color(80, 200, 220, 255)
        val success = Color(47, 102, 47, 255)
        val danger = Color(115, 41, 41, 255)
    }

    private lateinit var scrollComponent: ScrollComponent
    private lateinit var listContainer: UIContainer
    private lateinit var commandField: KeybindText
    private lateinit var keyButton: KeybindButton

    init {
        buildGui()
    }

    override fun onScreenClose() {
        super.onScreenClose()
        KeyShortcuts.dataUtils.save()
    }

    override fun onKeyPressed(keyCode: Int, typedChar: Char, modifiers: UKeyboard.Modifiers?) {
        if (keyCode > 0) keyButton.handleKeyPress(keyCode)
        if (!keyButton.listening && keyCode != 255) super.onKeyPressed(keyCode, typedChar, modifiers)
    }

    override fun onKeyReleased(keyCode: Int, typedChar: Char, modifiers: UKeyboard.Modifiers?) {
        if (keyCode > 0) keyButton.handleKeyRelease(keyCode)
        if (!keyButton.listening && keyCode != 255) super.onKeyReleased(keyCode, typedChar, modifiers)
    }

    private fun buildGui() {
        val border = createBlock(4f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 70.percent()
            height = 80.percent()
        }.setColor(theme.accent2) childOf window

        val main = createBlock(4f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 100.percent() - 2.pixels()
            height = 100.percent() - 2.pixels()
        }.setColor(theme.bg) childOf border

        createHeader(main)
        createContent(main)
        createFooter(main)
        renderBindings()
    }

    private fun createHeader(parent: UIComponent) {
        val header = UIContainer().constrain {
            x = 0.percent()
            y = 0.percent()
            width = 100.percent()
            height = 40.pixels()
        } childOf parent

        UIText("§lKey Shortcuts").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 1.8.pixels()
        }.setColor(theme.accent) childOf header

        createBlock(0f).constrain {
            x = 0.percent()
            y = 100.percent() - 1.pixels()
            width = 100.percent()
            height = 1.pixels()
        }.setColor(theme.accent2) childOf header
    }

    private fun createContent(parent: UIComponent) {
        val contentPanel = UIContainer().constrain {
            x = 8.pixels()
            y = 48.pixels()
            width = 100.percent() - 16.pixels()
            height = 100.percent() - 120.pixels()
        } childOf parent

        scrollComponent = ScrollComponent().constrain {
            x = 4.pixels()
            y = 4.pixels()
            width = 100.percent() - 8.pixels()
            height = 100.percent() - 8.pixels()
        } childOf contentPanel

        listContainer = UIContainer().constrain {
            width = 100.percent()
            height = ChildHeightConstraint(4f)
        } childOf scrollComponent
    }

    private fun createFooter(parent: UIComponent) {
        val footer = UIContainer().constrain {
            x = 8.pixels()
            y = 100.percent() - 40.pixels()
            width = 100.percent() - 16.pixels()
            height = 64.pixels()
        } childOf parent

        createBlock(0f).constrain {
            x = 0.percent()
            y = 0.percent()
            width = 100.percent()
            height = 1.pixels()
        }.setColor(theme.accent2) childOf footer

        commandField = KeybindText("", "To run (e.g., /help or hello world)").constrain {
            x = 0.pixels()
            y = 8.pixels()
            width = 61.percent()
            height = 24.pixels()
        } childOf footer

        keyButton = KeybindButton().constrain {
            x = 62.percent()
            y = 9.pixels()
            width = 25.percent()
            height = 22.pixels()
        } childOf footer

        val addButton = createBlock(3f).constrain {
            x = 88.percent()
            y = 9.pixels()
            width = 12.percent()
            height = 22.pixels()
        }.setColor(Color(18, 24, 28, 255)) childOf footer

        addButton.onMouseEnter {
            if (commandField.text.isEmpty() || keyButton.keys.isEmpty()) {
                animate { setColorAnimation(Animations.OUT_EXP, 0.3f, theme.danger.toConstraint()) }
                return@onMouseEnter
            }
            animate { setColorAnimation(Animations.OUT_EXP, 0.3f, theme.success.toConstraint()) }
        }.onMouseLeave {
            animate { setColorAnimation(Animations.OUT_EXP, 0.3f, Color(18, 24, 28, 255).toConstraint()) }
        }.onMouseClick {
            addBinding()
        }

        UIText("Add Shortcut").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 0.8.pixels()
        }.setColor(theme.accent) childOf addButton
    }

    private fun renderBindings() {
        listContainer.clearChildren()
        val bindings = KeyShortcuts.bindings

        if (bindings.isEmpty()) {
            UIText("No keybinds configured...").constrain {
                x = CenterConstraint()
                y = 20.pixels()
                textScale = 1f.pixels()
            }.setColor(theme.accent2.withAlpha(128)) childOf listContainer
            return
        }

        bindings.forEachIndexed { index, binding ->
            createBindingRow(index, binding)
        }
    }

    private fun createBindingRow(index: Int, binding: KeybindEntry) {
        val row = createBlock(3f).constrain {
            x = 0.percent()
            y = CramSiblingConstraint(4f)
            width = 100.percent()
            height = 32.pixels()
        }.setColor(theme.element) childOf listContainer

        val keyDisplay = createBlock(3f).constrain {
            x = 8.pixels()
            y = 6.pixels()
            width = 150.pixels()
            height = 20.pixels()
        }.setColor(Color(18, 24, 28, 255)) childOf row

        UIText(getKeysName(binding.keys)).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 0.8.pixels()
        }.setColor(theme.accent) childOf keyDisplay

        val commandInput = KeybindText(binding.command).constrain {
            x = 166.pixels()
            y = 6.pixels()
            width = 68.percent()
            height = 20.pixels()
        } childOf row

        commandInput.onChange = { command ->
            updateBinding(index, binding.keys, command)
        }

        createDeleteButton(row, index)
    }

    private fun createDeleteButton(parent: UIComponent, index: Int) {
        val deleteButton = createBlock(3f).constrain {
            x = 100.percent() - 32.pixels()
            y = CenterConstraint()
            width = 24.pixels()
            height = 20.pixels()
        }.setColor(theme.element) childOf parent

        deleteButton.onMouseEnter {
            animate { setColorAnimation(Animations.OUT_EXP, 0.3f, theme.danger.toConstraint()) }
        }.onMouseLeave {
            animate { setColorAnimation(Animations.OUT_EXP, 0.3f, theme.element.toConstraint()) }
        }.onMouseClick { removeBinding(index) }

        UIText("✕").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 0.8.pixels()
        }.setColor(Color.RED.darker()) childOf deleteButton
    }

    private fun addBinding() {
        val command = commandField.text.trim()
        val keys = keyButton.keys.toList()

        if (command.isEmpty() || keys.isEmpty()) return

        if (KeyShortcuts.addBinding(keys, command)) {
            commandField.text = ""
            keyButton.keys.clear()
            keyButton.keyText.setText("None")
            renderBindings()
        }
    }

    private fun updateBinding(index: Int, keys: List<Int>, command: String) {
        if (KeyShortcuts.updateBinding(index, keys, command)) {
            renderBindings()
        }
    }

    private fun removeBinding(index: Int) {
        if (KeyShortcuts.removeBinding(index)) {
            renderBindings()
        }
    }

    private fun getKeysName(keyList: List<Int>): String {
        if (keyList.isEmpty()) return "None"
        return keyList.joinToString(" + ") { getKeyName(it) }
    }

    private fun Color.withAlpha(alpha: Int): Color = Color(red, green, blue, alpha)
}

fun getKeyName(keyCode: Int): String {
    return when {
        keyCode == 0 -> "None"
        keyCode < 0 -> {
            when (val button = -keyCode - 1) {
                0 -> "LMB"
                1 -> "RMB"
                2 -> "MMB"
                else -> "Mouse ${button + 1}"
            }
        }
        keyCode == GLFW.GLFW_KEY_SPACE -> "Space"
        keyCode == GLFW.GLFW_KEY_ENTER -> "Enter"
        keyCode == GLFW.GLFW_KEY_TAB -> "Tab"
        keyCode == GLFW.GLFW_KEY_BACKSPACE -> "Backspace"
        keyCode == GLFW.GLFW_KEY_DELETE -> "Delete"
        keyCode == GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift"
        keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift"
        keyCode == GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl"
        keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl"
        keyCode == GLFW.GLFW_KEY_LEFT_ALT -> "LAlt"
        keyCode == GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt"
        keyCode in GLFW.GLFW_KEY_A..GLFW.GLFW_KEY_Z -> ('A' + (keyCode - GLFW.GLFW_KEY_A)).toString()
        keyCode in GLFW.GLFW_KEY_0..GLFW.GLFW_KEY_9 -> (keyCode - GLFW.GLFW_KEY_0).toString()
        keyCode in GLFW.GLFW_KEY_F1..GLFW.GLFW_KEY_F25 -> "F${keyCode - GLFW.GLFW_KEY_F1 + 1}"
        else -> KnitInputs.getDisplayName(keyCode)
    }
}