package meowing.zen.features.slayers

import meowing.zen.Zen
import meowing.zen.Zen.Companion.prefix
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.EntityEvent
import meowing.zen.features.Feature
import meowing.zen.utils.ChatUtils
import meowing.zen.utils.TickUtils
import meowing.zen.utils.Utils.removeFormatting
import java.util.regex.Pattern

@Zen.Module
object VengDamage : Feature("vengdmg") {
    private var nametagID = -1
    private val veng = Pattern.compile("^\\d+(,\\d+)*ﬗ$")
    fun handleNametagUpdate(entityId: Int) { nametagID = entityId }

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("Slayers", "Vengeance damage tracker", ConfigElement(
                "vengdmg",
                "Vengeance damager tracker",
                ElementType.Switch(false)
            ), isSectionToggle = true)
    }

    override fun initialize() {
        register<EntityEvent.Join> { event ->
            if (nametagID == -1) return@register

            TickUtils.scheduleServer(2) {
                val entityName = event.entity.name.string?.removeFormatting() ?: return@scheduleServer
                val vengMatch = veng.matcher(entityName)
                if (!vengMatch.matches()) return@scheduleServer

                val spawnedEntity = world?.getEntityById(event.entity.id) ?: return@scheduleServer
                val nametagEntity = world?.getEntityById(nametagID) ?: return@scheduleServer

                if (spawnedEntity.distanceTo(nametagEntity) > 5) return@scheduleServer

                val numStr = vengMatch.group(0).replace("ﬗ", "").replace(",", "")
                val num = numStr.toLongOrNull() ?: return@scheduleServer

                if (num > 500000)
                    ChatUtils.addMessage("$prefix §fVeng DMG: §c${vengMatch.group(0).replace("ﬗ", "")}")
            }
        }
    }
}