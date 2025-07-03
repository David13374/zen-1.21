package meowing.zen.feats.slayers

import meowing.zen.Zen.Companion.mc
import meowing.zen.events.EntityEvent
import meowing.zen.events.EventBus
import meowing.zen.events.RenderEvent
import meowing.zen.feats.Feature
import meowing.zen.utils.RenderUtils
import java.awt.Color

object lasertimer : Feature("lasertimer") {
    private var bossID = 0
    private val totaltime = 8.2
    private val renderCall: EventBus.EventCall = EventBus.register<RenderEvent.WorldPostEntities> ({ renderString() }, false)

    override fun initialize() {
        register<EntityEvent.Leave> { event ->
            if (event.entity.id == bossID) {
                bossID = 0
                renderCall.unregister()
            }
        }
    }

    fun handleSpawn(entityID: Int) {
        bossID = entityID - 3
        renderCall.register()
    }

    fun renderString() {
        val ent = mc.world?.getEntityById(bossID) ?: return
        val ridingentity = ent.vehicle ?: return
        val time = maxOf(0.0, totaltime - (ridingentity.age / 20.0))
        val text = "§bLaser: §c${"%.1f".format(time)}"

        RenderUtils.renderString(text, ent.pos, Color.WHITE.rgb, 2.0f, 1.0f)
    }
}