package com.sarani.carobd.core.protocol

object FrameParser {

    /**
     * Cleans raw output from ELM327 adapter by stripping prompt symbols,
     * echoed commands, and terminal control characters.
     */
    fun cleanResponse(raw: String): String {
        return raw.replace(">", "")
            .replace("\r", " ")
            .replace("\n", " ")
            .replace("SEARCHING...", "")
            .replace("BUS INIT: ...", "")
            .replace("BUS INIT: OK", "")
            .replace("BUS INIT: ERROR", "")
            .trim()
    }

    /**
     * Checks if response indicates an error or no data.
     */
    fun isErrorOrNoData(response: String): Boolean {
        val upper = response.uppercase()
        return upper.contains("NO DATA") ||
                upper.contains("UNABLE TO CONNECT") ||
                upper.contains("BUS INIT: ERROR") ||
                upper.contains("CAN ERROR") ||
                upper.contains("BUS BUSY") ||
                upper.contains("STOPPED") ||
                upper.contains("?") ||
                upper.contains("ERR")
    }

    /**
     * Extracts byte list from hexadecimal string.
     * Example: "41 0C 1A F8" -> [0x41, 0x0C, 0x1A, 0xF8]
     */
    fun extractHexBytes(response: String): List<Int> {
        val cleaned = cleanResponse(response)
        if (isErrorOrNoData(cleaned)) return emptyList()

        val tokens = cleaned.split("\\s+".toRegex())
        val bytes = mutableListOf<Int>()
        for (token in tokens) {
            val hex = token.trim()
            if (hex.length == 2) {
                hex.toIntOrNull(16)?.let { bytes.add(it) }
            } else if (hex.length == 4) {
                hex.substring(0, 2).toIntOrNull(16)?.let { bytes.add(it) }
                hex.substring(2, 4).toIntOrNull(16)?.let { bytes.add(it) }
            }
        }
        return bytes
    }

    /**
     * Parses standard Mode 03 response into DTC strings.
     * Response format: "43 01 33 00 00 00" -> P0133
     */
    fun parseDtcResponse(response: String): List<String> {
        val bytes = extractHexBytes(response)
        if (bytes.size < 2) return emptyList()

        // Expect Mode 03 response header 0x43 or 0x47
        val startIndex = if (bytes[0] == 0x43 || bytes[0] == 0x47) 1 else 0
        val dtcCodes = mutableListOf<String>()

        var i = startIndex
        while (i + 1 < bytes.size) {
            val b1 = bytes[i]
            val b2 = bytes[i + 1]

            // 0x0000 is padding / no code
            if (b1 != 0 || b2 != 0) {
                val typePrefix = when ((b1 and 0xC0) ushr 6) {
                    0 -> "P" // Powertrain
                    1 -> "C" // Chassis
                    2 -> "B" // Body
                    3 -> "U" // Network
                    else -> "P"
                }

                val digit1 = (b1 and 0x30) ushr 4
                val digit2 = b1 and 0x0F
                val digit3 = (b2 and 0xF0) ushr 4
                val digit4 = b2 and 0x0F

                val code = "$typePrefix$digit1$digit2$digit3$digit4"
                if (!dtcCodes.contains(code)) {
                    dtcCodes.add(code)
                }
            }
            i += 2
        }

        return dtcCodes
    }
}
