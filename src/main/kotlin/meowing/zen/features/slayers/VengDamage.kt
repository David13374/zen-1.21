package meowing.zen.features.slayers

import meowing.zen.Zen
import meowing.zen.Zen.Companion.prefix
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.EntityEvent
import meowing.zen.events.SkyblockEvent
import meowing.zen.features.Feature
import meowing.zen.utils.ChatUtils
import meowing.zen.utils.TickUtils
import meowing.zen.utils.Utils.removeFormatting
import net.minecraft.util.math.Vec3d
import java.util.regex.Pattern

@Zen.Module
object VengDamage : Feature("vengdmg", true) {
    private var nametagID = -1
    private val veng = Pattern.compile("^\\d+(,\\d+)*ﬗ$")

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("Slayers", "Vengeance damage tracker", ConfigElement(
                "vengdmg",
                "Vengeance damager tracker",
                ElementType.Switch(false)
            ), isSectionToggle = true)
    }

    override fun initialize() {
        register<SkyblockEvent.Slayer.Spawn> { event ->
            nametagID = event.entityID
        }

        register<SkyblockEvent.DamageSplash> { event ->
            if (nametagID == -1) return@register

            val entityName = event.originalName.removeFormatting()
            val vengMatch = veng.matcher(entityName)
            if (!vengMatch.matches()) return@register
            val name = vengMatch.group(0).replace("ﬗ", "")

            val nametagEntity = world?.getEntityById(nametagID) ?: return@register
            if (event.entity.distanceTo(nametagEntity) > 5) return@register

            val numStr = name.replace(",", "")
            val num = numStr.toLongOrNull() ?: return@register

            if (num > 500000) ChatUtils.addMessage("$prefix §fVeng DMG: §c${name}")
        }
    }
}