package meowing.zen.features.meowing

import meowing.zen.Zen
import meowing.zen.features.Feature
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.EntityEvent
import meowing.zen.utils.Utils
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.sound.SoundEvents

@Zen.Module
object MeowDeathSounds : Feature("meowdeathsounds") {
    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("Meowing", "Meow Death Sounds", ConfigElement(
                "meowdeathsounds",
                "Meow Death Sounds",
                ElementType.Switch(false)
            ), isSectionToggle = true)
            .addElement("Meowing", "Meow Death Sounds", "", ConfigElement(
                "",
                null,
                ElementType.TextParagraph("Plays a meow sound when a mob dies.")
            ))
    }

    override fun initialize() {
        register<EntityEvent.Death> { event ->
            val entity = event.entity
            if (entity is ArmorStandEntity || entity.isInvisible) return@register
            Utils.playSound(SoundEvents.ENTITY_CAT_AMBIENT, 0.8f, 1.0f)
        }
    }
}