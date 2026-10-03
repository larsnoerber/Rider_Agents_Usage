package io.github.larsnoerber.agentsusage.core.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val resetFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH)
    .withZone(ZoneId.systemDefault())

internal fun formatResetTime(epochSeconds: Long): String = resetFormatter.format(Instant.ofEpochSecond(epochSeconds))

internal fun resetCountdown(epochSeconds: Long): String {
    val seconds = (epochSeconds - System.currentTimeMillis() / 1_000).coerceAtLeast(0)
    return "Resets in %02d:%02d".format(Locale.ENGLISH, seconds / 3_600, (seconds % 3_600) / 60)
}
