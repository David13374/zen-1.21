package xyz.meowing.zen.features.dungeons

import xyz.meowing.knit.api.KnitChat
import xyz.meowing.zen.annotations.Module
import xyz.meowing.zen.config.ConfigDelegate
import xyz.meowing.zen.config.ui.types.ElementType
import xyz.meowing.zen.events.core.ChatEvent
import xyz.meowing.zen.features.Feature
import xyz.meowing.zen.managers.config.ConfigElement
import xyz.meowing.zen.managers.config.ConfigManager
import xyz.meowing.zen.utils.Utils.removeFormatting

@Module
object LeapAnnounce : Feature("leapannounce") {
    private val regex = "^You have teleported to (.+)".toRegex()
    private val leapmessage by ConfigDelegate<String>("leapmessage")

    override fun addConfig() {
        ConfigManager
            .addFeature("Leap announce", "", "Dungeons", ConfigElement(
                "leapannounce",
                ElementType.Switch(false)
            ))
            .addFeatureOption("Leap announce message", "Leap announce message", "Options", ConfigElement(
                "leapmessage",
                ElementType.TextInput("Leaping to", "Leaping to")
            ))
    }

    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            val result = regex.find(event.message.string.removeFormatting())
            if (result != null) KnitChat.sendCommand("pc $leapmessage ${result.groupValues[1]}")
        }
    }
}