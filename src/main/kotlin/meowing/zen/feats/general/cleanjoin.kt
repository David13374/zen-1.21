package meowing.zen.feats.general

import meowing.zen.events.ChatReceiveEvent
import meowing.zen.feats.Feature
import meowing.zen.utils.ChatUtils
import java.util.regex.Pattern

object guildjoinleave : Feature("guildjoinleave") {
    private val guildPattern = Pattern.compile("^§2Guild > §r(§[a-f0-9])(\\w+) §r§e(\\w+)\\.§r$")

    override fun initialize() {
        register<ChatReceiveEvent> { event ->
            if (event.overlay) return@register
            println(event.message!!.string)
            val m = guildPattern.matcher(event.message!!.string)
            if (m.matches()) {
                event.cancel()
                val color = m.group(1) ?: ""
                val user = m.group(2) ?: ""
                val action = m.group(3) ?: ""
                val message = when (action) {
                    "joined" -> "§8G §a>> $color$user"
                    "left" -> "§8G §c<< $color$user"
                    else -> return@register
                }
                ChatUtils.addMessage(message)
            }
        }
    }
}

object friendjoinleave : Feature("friendjoinleave") {
    private val friendPattern = Pattern.compile("^§aFriend > §r(§[a-f0-9])(\\w+) §r§e(\\w+)\\.§r$")

    override fun initialize() {
        register<ChatReceiveEvent> { event ->
            if (event.overlay) return@register
            println(event.message!!.string)
            val m = friendPattern.matcher(event.message!!.string)
            if (m.matches()) {
                event.cancel()
                val color = m.group(1) ?: ""
                val user = m.group(2) ?: ""
                val action = m.group(3) ?: ""
                val message = when (action) {
                    "joined" -> "§8F §a>> $color$user"
                    "left" -> "§8F §c<< $color$user"
                    else -> return@register
                }
                ChatUtils.addMessage(message)
            }
        }
    }
}