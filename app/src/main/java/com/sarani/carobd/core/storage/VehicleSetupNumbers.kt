package com.sarani.carobd.core.storage

/** Numeric metadata entered on English, Persian or Arabic keyboards; never supplies a default. */
internal object VehicleSetupNumbers {
    private fun normalize(input: String): String? {
        val text = input.trim()
        if (text.isEmpty() || text.length > 64) return null
        return buildString {
            for (char in text) append(when (char) {
                in '0'..'9', '.' -> char
                in '۰'..'۹' -> '0' + (char - '۰')
                in '٠'..'٩' -> '0' + (char - '٠')
                '٫' -> '.'
                else -> return null // Never guess ambiguous thousands/group separators or exponents.
            })
        }
    }

    fun tankLitres(input: String): Float? = normalize(input)?.takeIf { it.matches(Regex("[0-9]+(?:\\.[0-9]+)?")) }
        ?.toFloatOrNull()?.takeIf { it.isFinite() && it > 0 }

    fun odometerKm(input: String): Int? = normalize(input)?.takeIf { it.all { char -> char in '0'..'9' } }
        ?.toIntOrNull()?.takeIf { it >= 0 }
}
