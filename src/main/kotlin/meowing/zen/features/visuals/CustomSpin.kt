package meowing.zen.features.visuals

import meowing.zen.Zen
import meowing.zen.config.ConfigDelegate
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.RenderEvent
import meowing.zen.features.Feature
import net.minecraft.util.math.RotationAxis

@Zen.Module
object CustomSpin : Feature("customspin") {
    private val customspinspeed by ConfigDelegate<Double>("customspinspeed")
    private val spineveryone by ConfigDelegate<Boolean>("spineveryone")
    private val spindirection by ConfigDelegate<Int>("spindirection")

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("Visuals", "Custom spin", ConfigElement(
                "customspin",
                null,
                ElementType.Switch(false)
            ), isSectionToggle = true)
            .addElement("Visuals", "Custom spin", "Options", ConfigElement(
                "spineveryone",
                "Spin everyone",
                ElementType.Switch(true)
            ))
            .addElement("Visuals", "Custom spin", "Options", ConfigElement(
                "spindirection",
                "Custom spin direction",
                ElementType.Dropdown(listOf("Right", "Left"), 1)
            ))
            .addElement("Visuals", "Custom spin", "Options", ConfigElement(
                "customspinspeed",
                "Custom spin speed",
                ElementType.Slider(1.0, 20.0, 5.0, true)
            ))
    }

    override fun initialize() {
        register<RenderEvent.Player.Pre> { event ->
            if (spineveryone || event.entity.id == player?.id) {
                event.matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(getRotation()))
            }
        }
    }

    /*
     * Modified from NoammAddons code
     * Under GPL 3.0 License
     */
    private fun getRotation(): Float {
        val millis = System.currentTimeMillis() % 4000
        val fraction = millis / 4000f
        val angle = (fraction * 360f) * customspinspeed.toFloat()
        return if (spindirection == 0) angle - 180f else 180f - angle
    }
}