package meowing.zen.features.slayers

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import meowing.zen.Zen
import meowing.zen.Zen.Companion.prefix
import meowing.zen.api.SlayerTracker
import meowing.zen.config.ConfigDelegate
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.RenderEvent
import meowing.zen.events.SkyblockEvent
import meowing.zen.features.Feature
import meowing.zen.hud.HUDManager
import meowing.zen.utils.ChatUtils
import meowing.zen.utils.CommandUtils
import meowing.zen.utils.Render2D
import meowing.zen.utils.Utils.formatNumber
import meowing.zen.utils.TimeUtils
import meowing.zen.utils.TimeUtils.millis
import meowing.zen.utils.Utils.toFormattedDuration
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.gui.DrawContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Zen.Module
object SlayerStats : Feature("slayerstats", true) {
    private const val name = "SlayerStats"
    private val slayertimer by ConfigDelegate<Boolean>("slayertimer")
    private val slayerStatsLines by ConfigDelegate<Set<Int>>("slayerstatslines")

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("Slayers", "Slayer stats", ConfigElement(
                "slayerstats",
                "Slayer stats",
                ElementType.Switch(false)
            ), isSectionToggle = true)
            .addElement("Slayers", "Slayer stats", "", ConfigElement(
                "",
                null,
                ElementType.TextParagraph("Shows slayer statistics such as total bosses killed, bosses per hour, and average kill time. §c/slayerstats reset §rto reset stats. Requires §eSlayer Timer§r to be enabled.")
            ))
            .addElement("Slayers", "Slayer stats", "Options", ConfigElement(
                "slayerstatslines",
                "",
                ElementType.MultiCheckbox(
                    options = listOf("Show Bosses Killed", "Show Bosses/hr", "Show Average kill time", "Show Average spawn time", "Show Total Session time", "Show XP/hr"),
                    default = setOf(0, 1, 4, 5)
                )
            ))
    }

    override fun initialize() {
        HUDManager.register("SlayerStats", "$prefix §f§lSlayer Stats: \n§7> §bBosses Killed§f: §c15\n§7> §bBosses/hr§f: §c12\n§7> §bAvg. kill§f: §c45.2s")

        register<RenderEvent.HUD> {
            if (HUDManager.isEnabled("SlayerStats")) render(it.context)
        }

        register<SkyblockEvent.Slayer.Death> {
            if (!slayertimer) {
                ChatUtils.addMessage("$prefix §cYou must enable the §eSlayer Timer§c feature for Slayer Stats to work.")
            }
        }
    }

    private fun getBPH(): Int {
        if (SlayerTracker.sessionBossKills == 0) return 0

        val avgTotal = ((SlayerTracker.totalKillTime + SlayerTracker.totalSpawnTime).millis / SlayerTracker.sessionBossKills / 1000.0) // Avg Total Time in seconds
        val bph = (3600.0 / avgTotal).toInt()
        return bph
    }

    fun reset() {
        SlayerTracker.reset()
        ChatUtils.addMessage("$prefix §fSlayer stats reset!")
    }

    private fun render(context: DrawContext) {
        val x = HUDManager.getX(name)
        val y = HUDManager.getY(name)
        val scale = HUDManager.getScale(name)
        val lines = getLines()

        if (lines.isNotEmpty()) {
            var currentY = y
            for (line in lines) {
                Render2D.renderString(context, line, x, currentY, scale)
                currentY += fontRenderer.fontHeight + 2
            }
        }
    }

    private fun getLines(): List<String> {
        if (SlayerTracker.mobLastKilledAt.since.inWholeMinutes > 5 || SlayerTracker.mobLastKilledAt.isZero) {
            return emptyList()
        }

        val list = mutableListOf("$prefix §f§lSlayer Stats: ")

        if (slayerStatsLines.contains(4)) {
            if (SlayerTracker.sessionStart.isZero) {
                list.add(" §7> §bSession time§f: §c-")
            } else {
                val pauseMark = SlayerTracker.pauseStart
                val totalTime = TimeUtils.now - SlayerTracker.sessionStart - (pauseMark?.since ?: Duration.ZERO) - SlayerTracker.totalSessionPaused.milliseconds
                val timeString = totalTime.millis.toFormattedDuration(false)
                list.add(" §7> §bSession time§f: §c$timeString" + if (SlayerTracker.isPaused) " §7(Paused)" else "")
            }
        }

        slayerStatsLines.sorted().forEach { line ->
            when (line) {
                0 -> list.add(" §7> §bBosses Killed§f: §c${SlayerTracker.sessionBossKills}")
                1 -> list.add(" §7> §bBosses/hr§f: §c${if (SlayerTracker.sessionBossKills == 0) "-" else getBPH()}")
                2 -> {
                    val avgKill = if (SlayerTracker.sessionBossKills == 0) "-"
                    else (SlayerTracker.totalKillTime.millis / SlayerTracker.sessionBossKills / 1000.0).format(1) + "s"
                    list.add(" §7> §bAvg. kill§f: §c$avgKill")
                }
                3 -> {
                    val avgSpawn = if (SlayerTracker.sessionBossKills == 0) "-"
                    else (SlayerTracker.totalSpawnTime.millis / SlayerTracker.sessionBossKills / 1000.0).format(1) + "s"
                    list.add(" §7> §bAvg. spawn§f: §c$avgSpawn")
                }
                5 -> {
                    val xpPH = getBPH() * SlayerTracker.xpPerKill
                    list.add(" §7> §bXP/hr§f: §c${xpPH.formatNumber()} XP")
                }
            }
        }

        return list
    }
    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
}

@Zen.Command
object SlayerStatsCommand : CommandUtils(
    "slayerstats",
    listOf("zenslayerstats")
) {
    override fun execute(context: CommandContext<FabricClientCommandSource>): Int {
        ChatUtils.addMessage("$prefix §fPlease use §c/slayerstats reset")
        return 1
    }

    override fun buildCommand(builder: LiteralArgumentBuilder<FabricClientCommandSource>) {
        builder.then(
            ClientCommandManager.argument("action", StringArgumentType.string())
                .executes { context ->
                    val action = StringArgumentType.getString(context, "action")
                    if (action == "reset") SlayerStats.reset()
                    else ChatUtils.addMessage("$prefix §fPlease use §c/slayerstats reset")
                    1
                }
        )
    }
}