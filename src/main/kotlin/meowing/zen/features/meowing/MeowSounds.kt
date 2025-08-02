package meowing.zen.features.meowing

import meowing.zen.Zen
import meowing.zen.features.Feature
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.ChatEvent
import meowing.zen.utils.Utils
import meowing.zen.utils.Utils.removeFormatting
import net.minecraft.sound.SoundEvents

@Zen.Module
object MeowSounds : Feature("meowsounds") {
    private val meowRegex = Regex("(?:Guild|Party|Co-op|From|To)? ?>? ?(?:\\[.+?])? ?[a-zA-Z0-9_]+ ?(?:\\[.+?])?: (.+)")

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("Meowing", "Meow Sounds", ConfigElement(
                "meowsounds",
                "Meow Sounds",
                ElementType.Switch(false)
            ), isSectionToggle = true)
    }

    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            val content = event.message.string.removeFormatting().lowercase()
            val match = meowRegex.find(content) ?: return@register
            if (match.groups[1]?.value?.contains("meow", ignoreCase = true) != true) return@register

            Utils.playSound(SoundEvents.ENTITY_CAT_AMBIENT, 0.8f, 1.0f)
        }
    }
}