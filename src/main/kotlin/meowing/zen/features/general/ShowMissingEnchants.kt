package meowing.zen.features.general

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import meowing.zen.Zen
import meowing.zen.api.NEUApi
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigElement
import meowing.zen.config.ui.types.ElementType
import meowing.zen.events.ItemTooltipEvent
import meowing.zen.features.Feature
import meowing.zen.utils.ItemUtils.extraAttributes
import meowing.zen.utils.Utils.removeFormatting
import net.minecraft.client.util.InputUtil.GLFW_KEY_LEFT_SHIFT
import net.minecraft.nbt.NbtElement
import net.minecraft.text.Text
import org.apache.commons.lang3.StringUtils
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetKey

@Zen.Module
object ShowMissingEnchants : Feature("showmissingenchants") {
    private var enchantsData: JsonObject? = null
    private var enchantPools: JsonArray? = null
    private var poolIgnoreCache = mutableMapOf<Set<String>, Set<String>>()
    private var itemEnchantCache = mutableMapOf<String, JsonArray?>()
    private var tooltipCache = mutableMapOf<ItemCacheKey, List<Text>>()

    data class ItemCacheKey(
        val itemUuid: String,
        val enchantIds: Set<String>
    )

    override fun addConfig(configUI: ConfigUI): ConfigUI {
        return configUI
            .addElement("General", "Show Missing Enchants", ConfigElement(
                "showmissingenchants",
                "Show Missing Enchants",
                ElementType.Switch(false)
            ), isSectionToggle = true)
    }

    override fun initialize() {
        loadConstants()
        register<ItemTooltipEvent> { event ->
            if (glfwGetKey(window.handle, GLFW_KEY_LEFT_SHIFT) != GLFW_PRESS || enchantsData == null || enchantPools == null) return@register
            val extraAttributes = event.stack.extraAttributes ?: return@register
            if (!extraAttributes.contains("enchantments") || extraAttributes.get("enchantments")?.type != NbtElement.COMPOUND_TYPE) return@register
            val enchantments = extraAttributes.getCompound("enchantments").orElse(null) ?: return@register
            val enchantIds = enchantments.keys
            if (enchantIds.isEmpty()) return@register
            val itemUuid = extraAttributes.getString("uuid")
            if (itemUuid.isEmpty) return@register

            val cacheKey = ItemCacheKey(itemUuid.toString(), enchantIds)
            tooltipCache[cacheKey]?.let { cachedLines ->
                event.lines.clear()
                event.lines.addAll(cachedLines)
                return@register
            }

            if (!hasEnchantInTooltip(event.lines, enchantIds)) return@register
            val allItemEnchs = getItemEnchantsFromCache(event.lines) ?: return@register

            val emptyLineIndex = findEmptyLineAfterEnchants(event.lines, enchantIds)
            if (emptyLineIndex == -1) return@register

            val ignoreSet = poolIgnoreCache.getOrPut(enchantIds) { getIgnoreFromPool(enchantIds) }
            val missing = allItemEnchs.asSequence()
                .map { it.asString }
                .filter { !it.startsWith("ultimate_") && !ignoreSet.contains(it) && !enchantIds.contains(it) }
                .toList()

            if (missing.isNotEmpty()) {
                addMissingEnchantsToTooltip(event.lines, missing, emptyLineIndex)
                tooltipCache[cacheKey] = ArrayList(event.lines)
                if (tooltipCache.size > 50) tooltipCache.clear()
            }
        }
    }

    private fun loadConstants() {
        try {
            val constants = NEUApi.NeuConstantData.getData().getAsJsonObject("enchants")
            enchantsData = constants?.getAsJsonObject("enchants")
            enchantPools = constants?.getAsJsonArray("enchant_pools")
        } catch (e: Exception) {
            LOGGER.warn("Failed to load enchants in ShowMissingEnchants: $e")
        }
    }

    private fun getIgnoreFromPool(enchantIds: Set<String>): Set<String> {
        val ignoreFromPool = mutableSetOf<String>()
        enchantPools?.forEach { poolElement ->
            val poolEnchants = poolElement.asJsonArray.map { it.asString }.toSet()
            if (poolEnchants.any { enchantIds.contains(it) }) {
                ignoreFromPool.addAll(poolEnchants)
            }
        }
        return ignoreFromPool
    }

    private fun getItemEnchantsFromCache(tooltip: List<Text>): JsonArray? {
        val itemName = tooltip.lastOrNull { it.string.removeFormatting().trim().isNotEmpty() }
            ?.string?.removeFormatting()?.trim() ?: return null

        return itemEnchantCache.getOrPut(itemName) {
            enchantsData?.entrySet()?.find { (key, _) ->
                itemName.contains(key, ignoreCase = true)
            }?.value?.asJsonArray
        }
    }

    private fun hasEnchantInTooltip(tooltip: List<Text>, enchantIds: Set<String>): Boolean {
        val enchantNames by lazy { enchantIds.map { StringUtils.capitalize(it.replace("_", " ")) } }
        return tooltip.any { line -> enchantNames.any { line.string.contains(it) } }
    }

    private fun findEmptyLineAfterEnchants(tooltip: List<Text>, enchantIds: Set<String>): Int {
        val enchantNames by lazy { enchantIds.map { StringUtils.capitalize(it.replace("_", " ")) } }
        var foundEnchantSection = false

        for (i in tooltip.indices) {
            val line = tooltip[i].string
            if (enchantNames.any { line.contains(it) }) {
                foundEnchantSection = true
            } else if (foundEnchantSection && line.removeFormatting().trim().isEmpty()) {
                return i
            }
        }
        return -1
    }

    private fun addMissingEnchantsToTooltip(tooltip: MutableList<Text>, missing: List<String>, insertIndex: Int) {
        val lines = mutableListOf<Text>()
        lines.add(Text.of(""))

        val sb = StringBuilder("§cMissing: ")
        var lineLength = 9

        missing.forEachIndexed { index, enchantId ->
            val enchantName = StringUtils.capitalize(enchantId.replace("_", " "))
            val separator = if (index == 0) "" else ", "
            val newLength = lineLength + separator.length + enchantName.length

            if (index > 0 && newLength > 40) {
                lines.add(Text.of(sb.toString()))
                sb.clear().append("§7$enchantName")
                lineLength = enchantName.length
            } else {
                sb.append("$separator§7$enchantName")
                lineLength = newLength
            }
        }

        if (sb.isNotEmpty()) lines.add(Text.of(sb.toString()))
        tooltip.addAll(insertIndex, lines)
    }
}