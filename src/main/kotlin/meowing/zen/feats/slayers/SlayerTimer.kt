package meowing.zen.feats.slayers

import meowing.zen.Zen
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.ChatEvent
import meowing.zen.events.EntityEvent
import meowing.zen.events.EventBus
import meowing.zen.events.TickEvent
import meowing.zen.utils.ChatUtils
import meowing.zen.utils.Utils.removeFormatting
import meowing.zen.feats.Feature
import net.minecraft.entity.LivingEntity

@Zen.Module
object SlayerTimer : Feature("slayertimer") {
    @JvmField var BossId = -1
    @JvmField var isFighting = false

    private val fail = Regex("^ {2}SLAYER QUEST FAILED!$")
    private val questStart = Regex("^ {2}SLAYER QUEST STARTED!$")
    private var startTime = 0L
    private var spawnTime = 0L
    private var serverTicks = 0
    private var servertickcall: EventBus.EventCall = EventBus.register<TickEvent.Server> ({ serverTicks++ }, false)

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("Slayers", "General", ConfigElement(
                "slayertimer",
                "Slayer timer",
                "Sends a message in your chat telling you how long it took to kill your boss.",
                ElementType.Switch(false)
            ))
    }

    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            val text = event.message.string.removeFormatting()
            when {
                fail.matches(text) -> onSlayerFailed()
                questStart.matches(text) -> spawnTime = System.currentTimeMillis()
            }
        }

        register<EntityEvent.Death> { event ->
            if (event.entity is LivingEntity && event.entity.id == BossId && isFighting) {
                val timeTaken = System.currentTimeMillis() - startTime
                sendTimerMessage("You killed your boss", timeTaken, serverTicks)
                if (Zen.config.slayerstats) slayerstats.addKill(timeTaken)
                resetBossTracker()
            }
        }
    }

    fun handleBossSpawn(entityId: Int) {
        if (isFighting) return
        BossId = entityId - 3
        startTime = System.currentTimeMillis()
        isFighting = true
        serverTicks = 0
        servertickcall.register()
        resetSpawnTimer()
    }

    private fun onSlayerFailed() {
        if (!isFighting) return
        val timeTaken = System.currentTimeMillis() - startTime
        sendTimerMessage("Your boss killed you", timeTaken, serverTicks)
        resetBossTracker()
    }

    private fun sendTimerMessage(action: String, timeTaken: Long, ticks: Int) {
        val seconds = timeTaken / 1000.0
        val serverTime = ticks / 20.0
        val content = "§c[Zen] §f$action in §b${"%.2f".format(seconds)}s §7| §b${"%.2f".format(serverTime)}s"
        val hoverText = "§c${timeTaken}ms §f| §c${"%.0f".format(ticks.toFloat())} ticks"
        ChatUtils.addMessage(content, hoverText)
    }

    private fun resetBossTracker() {
        BossId = -1
        startTime = 0
        isFighting = false
        serverTicks = 0
        servertickcall.unregister()
    }

    private fun resetSpawnTimer() {
        if (spawnTime == 0L) return
        val spawnSeconds = (System.currentTimeMillis() - spawnTime) / 1000.0
        val content = "§c[Zen] §fYour boss spawned in §b${"%.2f".format(spawnSeconds)}s"
        ChatUtils.addMessage(content)
        spawnTime = 0
    }
}