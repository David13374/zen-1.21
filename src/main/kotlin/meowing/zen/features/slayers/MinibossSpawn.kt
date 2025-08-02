package meowing.zen.features.slayers

import meowing.zen.Zen
import meowing.zen.Zen.Companion.prefix
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.EntityEvent
import meowing.zen.events.WorldEvent
import meowing.zen.features.Feature
import meowing.zen.features.carrying.CarryCounter
import meowing.zen.utils.ChatUtils
import meowing.zen.utils.TickUtils
import meowing.zen.utils.Utils
import meowing.zen.utils.Utils.removeFormatting
import net.minecraft.sound.SoundEvents

@Zen.Module
object MinibossSpawn : Feature("minibossspawn") {
    private val entities = mutableListOf<Int>()
    private val names = listOf(
        "Atoned Revenant ", "Atoned Champion ", "Deformed Revenant ", "Revenant Champion ", "Revenant Sycophant ",
        "Mutant Tarantula ", "Tarantula Beast ", "Tarantula Vermin ",
        "Sven Alpha ", "Sven Follower ", "Pack Enforcer ",
        "Voidcrazed Maniac ", "Voidling Radical ", "Voidling Devotee "
    )
    private val regex = "\\d[\\d.,]*[kKmMbBtT]?❤?$".toRegex()

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("Slayers", "Miniboss spawn alert", ConfigElement(
                "minibossspawn",
                "Miniboss spawn alert",
                ElementType.Switch(false)
            ), isSectionToggle = true)
    }

    override fun initialize() {
        register<EntityEvent.Join> { event ->
            if (entities.contains(event.entity.id)) return@register
            if (CarryCounter.carryees.isEmpty() && SlayerTimer.spawnTime.isZero) return@register
            if (event.entity.distanceTo(player) > 10) return@register
            TickUtils.scheduleServer(2) {
                val entity = event.entity
                val name = entity.name?.string?.removeFormatting()?.replace(regex, "") ?: return@scheduleServer
                if (names.contains(name)) {
                    Utils.playSound(SoundEvents.ENTITY_CAT_AMBIENT, 1f, 1f)
                    ChatUtils.addMessage("$prefix §b$name§fspawned.")
                    entities.add(entity.id)
                }
            }
        }

        register<WorldEvent.Change> {
            entities.clear()
        }
    }
}