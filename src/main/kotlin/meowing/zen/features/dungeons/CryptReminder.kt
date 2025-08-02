package meowing.zen.features.dungeons

import meowing.zen.Zen
import meowing.zen.config.ConfigDelegate
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.ChatEvent
import meowing.zen.features.Feature
import meowing.zen.utils.ChatUtils
import meowing.zen.utils.DungeonUtils
import meowing.zen.utils.LocationUtils
import meowing.zen.utils.LoopUtils.setTimeout
import meowing.zen.utils.TitleUtils.showTitle
import meowing.zen.utils.Utils.removeFormatting

@Zen.Module
object CryptReminder : Feature("cryptreminder") {
    private val cryptreminderdelay by ConfigDelegate<Double>("cryptreminderdelay")

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("Dungeons", "Crypt reminder", "Options", ConfigElement(
                "cryptreminder",
                "Crypt reminder",
                ElementType.Switch(false)
            ), isSectionToggle = true)
            .addElement("Dungeons", "Crypt reminder", "Options", ConfigElement(
                "cryptreminderdelay",
                "Crypt reminder delay",
                ElementType.Slider(1.0, 5.0, 2.0, false)
            ))
    }

    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            if (event.message.string.removeFormatting() == "[NPC] Mort: Good luck.") {
                setTimeout(1000 * 60 * cryptreminderdelay.toLong()) {
                    if (DungeonUtils.getCryptCount() == 5 || !LocationUtils.checkArea("catacombs")) return@setTimeout
                    ChatUtils.command("/pc Zen » ${DungeonUtils.getCryptCount()}/5 crypts")
                    showTitle("§c${DungeonUtils.getCryptCount()}§7/§c5 §fcrypts", null, 3000)
                }
            }
        }
    }
}