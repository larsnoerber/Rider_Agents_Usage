package io.github.larsnoerber.agentsusage.ui

import com.intellij.ui.JBColor
import java.awt.Color

private val healthy = JBColor(Color(45, 130, 75), Color(100, 205, 135))
private val low = JBColor(Color(190, 125, 0), Color(255, 185, 75))
private val critical = JBColor(Color(198, 45, 45), Color(255, 110, 110))

internal fun usageBarColor(percent: Int?): Color = when {
    percent == null -> JBColor.GRAY
    percent < 20 -> critical
    percent < 50 -> low
    else -> healthy
}
