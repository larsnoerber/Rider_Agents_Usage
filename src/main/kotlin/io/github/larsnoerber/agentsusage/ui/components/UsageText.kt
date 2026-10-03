package io.github.larsnoerber.agentsusage.ui.components

import com.intellij.openapi.util.text.StringUtil

internal fun usageTooltip(title: String, lines: List<String>): String = buildString {
    append("<html><b>")
    append(StringUtil.escapeXmlEntities(title))
    append("</b>")
    lines.filter(String::isNotBlank).forEach {
        append("<br>")
        append(StringUtil.escapeXmlEntities(it))
    }
    append("</html>")
}

internal fun compactUsageMessage(message: String): String =
    if (message.length > 42) message.take(41) + "…" else message
