package meowing.zen.feats.dungeons

import meowing.zen.events.ChatEvent
import meowing.zen.feats.Feature
import meowing.zen.utils.ChatUtils
import meowing.zen.utils.Utils.removeFormatting
import java.util.regex.Pattern

object termtracker : Feature("termtracker", area = "catacombs") {
    private lateinit var completed: MutableMap<String, MutableMap<String, Int>>
    private val pattern = Pattern.compile("^(\\w{1,16}) (?:activated|completed) a (\\w+)! \\(\\d/\\d\\)$")

    override fun initialize() {
        completed = mutableMapOf()
        register<ChatEvent.Receive> { event ->
            val msg = event.message.string.removeFormatting()
            val matcher = pattern.matcher(msg)

            when {
                msg == "The Core entrance is opening!" -> {
                    completed.forEach { (user, data) ->
                        ChatUtils.addMessage("§c[Zen] §b$user§7 - §b${data["lever"] ?: 0} §flevers §7| §b${data["terminal"] ?: 0} §fterminals §7| §b${data["device"] ?: 0} §fdevices")
                    }
                }
                matcher.matches() -> {
                    val user = matcher.group(1)
                    val type = matcher.group(2)
                    if (type in listOf("terminal", "lever", "device"))
                        completed.getOrPut(user) { mutableMapOf() }[type] = (completed[user]?.get(type) ?: 0) + 1
                }
            }
        }
    }

    override fun onRegister() {
        if (this::completed.isInitialized) completed.clear()
    }

    override fun onUnregister() {
        if (this::completed.isInitialized) completed.clear()
    }
}