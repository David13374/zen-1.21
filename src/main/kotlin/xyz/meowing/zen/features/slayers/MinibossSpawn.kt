package xyz.meowing.zen.features.slayers

import xyz.meowing.zen.Zen
import xyz.meowing.zen.Zen.Companion.prefix
import xyz.meowing.zen.api.SlayerTracker
import xyz.meowing.zen.config.ConfigDelegate
import xyz.meowing.zen.config.ui.ConfigUI
import xyz.meowing.zen.config.ui.types.ConfigElement
import xyz.meowing.zen.config.ui.types.ElementType
import xyz.meowing.zen.events.EntityEvent
import xyz.meowing.zen.events.WorldEvent
import xyz.meowing.zen.features.Feature
import xyz.meowing.zen.features.slayers.carrying.CarryCounter
import xyz.meowing.zen.utils.ChatUtils
import xyz.meowing.zen.utils.Utils
import xyz.meowing.zen.utils.Utils.removeFormatting
import net.minecraft.sound.SoundEvents

@Zen.Module
object MinibossSpawn : Feature("minibossspawn", true) {
    private val carrycounter by ConfigDelegate<Boolean>("carrycounter")
    private val slayertimer by ConfigDelegate<Boolean>("slayertimer")
    private val entities = mutableListOf<Int>()
    private val names = listOf(
        "Atoned Revenant ", "Atoned Champion ", "Deformed Revenant ", "Revenant Champion ", "Revenant Sycophant ",
        "Mutant Tarantula ", "Tarantula Beast ", "Tarantula Vermin ",
        "Sven Alpha ", "Sven Follower ", "Pack Enforcer ",
        "Voidcrazed Maniac ", "Voidling Radical ", "Voidling Devotee "
    )
    private val regex = "\\d[\\d.,]*[kKmMbBtT]?❤?$".toRegex()

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        xyz.meowing.zen.ui.ConfigMenuManager
            .addFeature("Miniboss spawn alert", "Miniboss spawn alert", "Slayers", xyz.meowing.zen.ui.ConfigElement(
                "minibossspawn",
                ElementType.Switch(false)
            ))
        return configUI
    }

    override fun initialize() {
        register<EntityEvent.Metadata> { event ->
            if (entities.contains(event.packet.id)) return@register
            if ((carrycounter && CarryCounter.carryees.isEmpty()) && (slayertimer && SlayerTracker.questStartedAtTime.isZero)) return@register
            val name = event.name.removeFormatting().replace(regex, "")
            if (names.contains(name)) {
                Utils.playSound(SoundEvents.ENTITY_CAT_AMBIENT, 1f, 1f)
                ChatUtils.addMessage("$prefix §b$name§fspawned.")
                entities.add(event.packet.id)
            }
        }

        register<WorldEvent.Change> {
            entities.clear()
        }
    }
}