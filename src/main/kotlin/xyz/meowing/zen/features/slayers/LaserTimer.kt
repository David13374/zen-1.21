package xyz.meowing.zen.features.slayers

import xyz.meowing.knit.api.KnitClient.world
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.zen.annotations.Module
import xyz.meowing.zen.managers.config.ConfigElement
import xyz.meowing.zen.managers.config.ConfigManager
import xyz.meowing.zen.config.ui.types.ElementType
import xyz.meowing.zen.events.core.RenderEvent
import xyz.meowing.zen.events.core.SkyblockEvent
import xyz.meowing.zen.features.Feature
import xyz.meowing.zen.utils.Render3D
import java.awt.Color

@Module
object LaserTimer : Feature("lasertimer", true) {
    private var bossID = 0
    private val totaltime = 8.2

    override fun addConfig() {
        ConfigManager
            .addFeature("Laser phase timer", "Laser phase timer", "Slayers", ConfigElement(
                "lasertimer",
                ElementType.Switch(false)
            ))
    }


    override fun initialize() {
        createCustomEvent<RenderEvent.Entity.Post>("render") {
            drawString()
        }

        register<SkyblockEvent.Slayer.Spawn> { event ->
            bossID = event.entityID - 3
            registerEvent("render")
        }

        register<SkyblockEvent.Slayer.Death> {
            bossID = 0
            unregisterEvent("render")
        }

        register<SkyblockEvent.Slayer.Fail> {
            bossID = 0
            unregisterEvent("render")
        }

        register<SkyblockEvent.Slayer.Cleanup> {
            bossID = 0
            unregisterEvent("render")
        }
    }

    private fun drawString() {
        val ent = world?.getEntityById(bossID) ?: return
        if (player?.canSee(ent) != true) return
        val ridingentity = ent.vehicle ?: return
        val time = maxOf(0.0, totaltime - (ridingentity.age / 20.0))
        val text = "§bLaser: §c${"%.1f".format(time)}"

        Render3D.drawString(
            text,
            //#if MC >= 1.21.9
            //$$ ent.entityPos,
            //#else
            ent.pos,
            //#endif
            Color.WHITE.rgb,
            2.0f,
            1.0f
        )
    }
}