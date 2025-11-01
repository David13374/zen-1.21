package xyz.meowing.zen.features.dungeons

import xyz.meowing.zen.config.ui.types.ElementType
import xyz.meowing.zen.features.Feature
import xyz.meowing.zen.utils.TitleUtils
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.passive.BatEntity
import xyz.meowing.zen.annotations.Module
import xyz.meowing.zen.api.dungeons.DungeonAPI
import xyz.meowing.zen.api.location.SkyBlockIsland
import xyz.meowing.zen.events.core.EntityEvent
import xyz.meowing.zen.managers.config.ConfigElement
import xyz.meowing.zen.managers.config.ConfigManager

@Module
object BatDeathTitle : Feature("batdeadtitle", true, SkyBlockIsland.THE_CATACOMBS) {

    override fun addConfig() {
        ConfigManager
            .addFeature("Bat Death Title", "Shows a title when bats die in dungeons", "Dungeons", ConfigElement(
                "batdeadtitle",
                ElementType.Switch(false)
            ))
    }

    override fun initialize() {
        register<EntityEvent.Death> {
            if (it.entity is BatEntity && it.entity.vehicle !is ArmorStandEntity && !DungeonAPI.inBoss) {
                TitleUtils.showTitle("§cBat Dead!", null, 1000)
            }
        }
    }
}