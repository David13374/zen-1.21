package meowing.zen.config.ui.core

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.constraints.AdditiveConstraint
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.CramSiblingConstraint
import gg.essential.elementa.constraints.RelativeConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import meowing.zen.Zen.Companion.mc
import meowing.zen.config.ui.ConfigUI
import meowing.zen.config.ui.types.ConfigCategory
import meowing.zen.config.ui.types.ConfigSection
import meowing.zen.hud.HUDEditor
import meowing.zen.utils.TickUtils
import java.awt.Color

class UIBuilder(private val theme: ConfigTheme) {
    fun createCategoryButton(category: ConfigCategory, isActive: Boolean, onClick: () -> Unit): UIComponent {
        val button = UIRoundedRectangle(5f).constrain {
            x = 0.percent()
            y = CramSiblingConstraint(5f)
            width = 100.percent()
            height = 35.pixels()
        }.setColor(if (isActive) Color(theme.accent.red, theme.accent.green, theme.accent.blue, 40) else Color(0, 0, 0, 0))

        if (!isActive) {
            button.onMouseEnter {
                setColor(Color(theme.accent2.red, theme.accent2.green, theme.accent2.blue, 20))
            }.onMouseLeave {
                setColor(Color(0, 0, 0, 0))
            }.onMouseClick { onClick() }
        }

        UIText(category.name).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 1.3.pixels()
        }.setColor(if (isActive) theme.accent else theme.accent2) childOf button

        return button
    }

    fun createHudButton(): UIComponent {
        val button = UIRoundedRectangle(5f).constrain {
            x = CenterConstraint()
            y = 5.pixels(true)
            width = 90.percent()
            height = 35.pixels()
        }.setColor(Color(theme.accent.red, theme.accent.green, theme.accent.blue, 40))

        button.onMouseEnter {
            setColor(Color(theme.accent2.red, theme.accent2.green, theme.accent2.blue, 20))
        }.onMouseLeave {
            setColor(Color(theme.accent.red, theme.accent.green, theme.accent.blue, 40))
        }.onMouseClick {
            TickUtils.schedule(1) {
                mc.execute {
                    mc.setScreen(HUDEditor())
                }
            }
        }

        UIText("HUD Editor").constrain {
            x = AdditiveConstraint(CenterConstraint(), 5.pixels())
            y = CenterConstraint()
            textScale = 1.3.pixels()
        }.setColor(theme.accent2) childOf button

        return button
    }

    fun createSectionCard(section: ConfigSection, onClick: () -> Unit): UIComponent {
        val card = UIRoundedRectangle(5f).constrain {
            x = CramSiblingConstraint(15f)
            y = CramSiblingConstraint(15f)
            width = 23.percent()
            height = 80.pixels()
        }.setColor(Color(theme.element.red + 20, theme.element.green + 20, theme.element.blue + 20, 255))

        val innerCard = UIRoundedRectangle(5f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 99.2.percent()
            height = 98.5.percent()
        }.setColor(theme.element) childOf card

        innerCard
            .onMouseClick {
                onClick()
            }
            .onMouseEnter {
                if (ConfigUI.activePopup != null) return@onMouseEnter
                setColor(theme.elementHover)
                card.setColor(theme.accent)
            }
            .onMouseLeave {
                setColor(theme.element)
                card.setColor(Color(theme.element.red + 20, theme.element.green + 20, theme.element.blue + 20, 255))
            }

        UIWrappedText(section.name, centered = true).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 90.percent()
            textScale = 1.3.pixels()
        }.setColor(theme.accent) childOf innerCard

        return card
    }

    fun createPopupOverlay(onClose: () -> Unit): UIComponent {
        return UIRoundedRectangle(0f).constrain {
            x = 0.percent()
            y = 0.percent()
            width = 100.percent()
            height = 100.percent()
        }.setColor(Color(0, 0, 0, 150))
    }

    fun createPopup(): UIComponent {
        return UIRoundedRectangle(8f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 90.percent()
            height = 90.percent()
        }.setColor(theme.popup)
    }

    fun createPopupHeader(title: String): UIComponent {
        val header = UIRoundedRectangle(5f).constrain {
            x = CenterConstraint()
            y = 0.percent()
            width = 100.percent()
            height = 13.percent()
        }.setColor(theme.element)

        UIText(title).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 2.0.pixels()
        }.setColor(theme.accent) childOf header

        return header
    }

    fun createCloseButton(onClose: () -> Unit): UIComponent {
        return UIText("×").constrain {
            x = RelativeConstraint(1f) - 25.pixels()
            y = 10.pixels()
            textScale = 3.pixels()
        }.setColor(theme.accent2)
            .onMouseClick { onClose() }
            .onMouseEnter { setColor(theme.accent) }
            .onMouseLeave { setColor(theme.accent2) }
    }
}