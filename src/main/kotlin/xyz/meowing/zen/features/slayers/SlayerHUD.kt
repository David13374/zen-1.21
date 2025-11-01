package xyz.meowing.zen.features.slayers

import xyz.meowing.zen.config.ui.types.ElementType
import xyz.meowing.zen.features.Feature
import xyz.meowing.zen.hud.HUDManager
import xyz.meowing.zen.utils.Render2D.width
import xyz.meowing.zen.utils.Utils.removeFormatting
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.Entity
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.KnitClient.world
import xyz.meowing.zen.annotations.Module
import xyz.meowing.zen.events.core.GuiEvent
import xyz.meowing.zen.events.core.SkyblockEvent
import xyz.meowing.zen.managers.config.ConfigElement
import xyz.meowing.zen.managers.config.ConfigManager

@Module
object SlayerHUD : Feature("slayerhud", true) {
    private const val name = "Slayer HUD"
    private var timerEntity: Entity? = null
    private var hpEntity: Entity? = null
    private var bossID: Int? = null

    override fun addConfig() {
        ConfigManager
            .addFeature("Slayer HUD", "Slayer HUD", "Slayers", ConfigElement(
                "slayerhud",
                ElementType.Switch(false)
            ))
    }


    override fun initialize() {
        HUDManager.register(name, "§c02:59\n§c☠ §bVoidgloom Seraph IV §e64.2M§c❤")

        createCustomEvent<GuiEvent.Render.HUD>("render") {
            if (HUDManager.isEnabled(name)) render(it.context)
        }

        register<SkyblockEvent.Slayer.Spawn> { event ->
            val world = world ?: return@register
            bossID = event.entityID
            timerEntity = world.getEntityById(event.entityID - 1)
            hpEntity = world.getEntityById(event.entityID - 2)
            registerEvent("render")
        }

        register<SkyblockEvent.Slayer.Death> {
            unregisterEvent("render")
            bossID = null
        }

        register<SkyblockEvent.Slayer.Fail> {
            unregisterEvent("render")
            bossID = null
        }

        register<SkyblockEvent.Slayer.Cleanup> {
            unregisterEvent("render")
            bossID = null
        }
    }

    private fun render(context: DrawContext) {
        val timeText = timerEntity?.name ?: return
        val hpText = hpEntity?.name ?: return
        val x = HUDManager.getX(name)
        val y = HUDManager.getY(name)
        val scale = HUDManager.getScale(name)
        val matrices = context.matrices
        //#if MC >= 1.21.7
        //$$ matrices.pushMatrix()
        //$$ matrices.translate(x, y)
        //#else
        matrices.push()
        matrices.translate(x, y, 0f)
        //#endif

        val hpWidth = hpText.string.removeFormatting().width()
        val timeWidth = timeText.string.removeFormatting().width()

        context.drawText(client.textRenderer, timeText, (hpWidth - timeWidth) / 2, 0, -1, false)
        context.drawText(client.textRenderer, hpText, 0, 10, -1, false)

        //#if MC >= 1.21.7
        //$$ matrices.popMatrix()
        //#else
        matrices.pop()
        //#endif
    }
}