package xyz.meowing.zen.features.slayers

import net.minecraft.sound.SoundEvents
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.zen.Zen
import xyz.meowing.zen.Zen.Companion.prefix
import xyz.meowing.zen.config.ConfigDelegate
import xyz.meowing.zen.config.ui.types.ElementType
import xyz.meowing.zen.features.Feature
import xyz.meowing.zen.utils.Utils
import xyz.meowing.zen.utils.Utils.removeFormatting
import xyz.meowing.zen.config.ConfigElement
import xyz.meowing.zen.config.ConfigManager
import xyz.meowing.zen.events.ChatEvent
import xyz.meowing.zen.utils.TitleUtils

@Zen.Module
object MinibossSpawn : Feature("minibossspawn", true) {
    private val showTitle by ConfigDelegate<Boolean>("minibossspawntitle")

    override fun addConfig() {
        ConfigManager
            .addFeature("Miniboss spawn alert", "Miniboss spawn alert", "Slayers", ConfigElement(
                "minibossspawn",
                ElementType.Switch(false)
            ))
            .addFeatureOption("Show title on spawn", "", "Options", ConfigElement(
                "minibossspawntitle",
                ElementType.Switch(true)
            ))
    }

    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            val message = event.message.string.removeFormatting()

            if (message.contains("SLAYER MINI-BOSS") && message.contains("has spawned!")) {
                val minibossName = extractMinibossName(message) ?: return@register

                Utils.playSound(SoundEvents.ENTITY_CAT_AMBIENT, 1f, 1f)
                KnitChat.fakeMessage("$prefix §b$minibossName §fspawned.")

                if (showTitle) {
                    TitleUtils.showTitle("§b$minibossName", "§fMiniboss Spawned!", 2000, scale = 3.0f)
                }
            }
        }
    }

    private fun extractMinibossName(message: String): String? {
        val start = message.indexOf("MINI-BOSS", ignoreCase = true).takeIf { it != -1 } ?: return null
        val end = message.indexOf(" has", start, ignoreCase = true).takeIf { it != -1 } ?: message.length
        return message.substring(start + 10, end).trim().ifEmpty { null }
    }
}