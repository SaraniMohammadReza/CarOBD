package com.sarani.carobd.core.adapter.elm327

import com.sarani.carobd.core.adapter.command.AdapterError
import com.sarani.carobd.core.adapter.command.AdapterResponse

/**
 * State machine parser for ELM327 / STN stream buffers.
 * Detects prompts ('>'), cleans responses, filters out search messages, and classifies errors.
 */
object Elm327BufferParser {

    const val PROMPT_CHAR: Char = '>'

    /**
     * Checks if a raw buffer ends with the ELM327 prompt character '>'.
     */
    fun hasPrompt(buffer: String): Boolean = buffer.trimEnd().endsWith(PROMPT_CHAR)

    /**
     * Parses a complete buffer (which terminated with '>') for a given command.
     */
    fun parseBuffer(rawBuffer: String, commandSent: String, elapsedMs: Long): AdapterResponse {
        val trimmed = rawBuffer.trim()
        val withoutPrompt = if (trimmed.endsWith(PROMPT_CHAR)) {
            trimmed.substring(0, trimmed.length - 1).trim()
        } else {
            trimmed
        }

        // Split into lines by CR or LF
        val lines = withoutPrompt.split(Regex("[\r\n]+"))
            .map { it.trim() }
            .filter { line ->
                line.isNotEmpty() &&
                !line.equals(commandSent.trim(), ignoreCase = true) && // Remove command echo
                !line.equals("SEARCHING...", ignoreCase = true) // Remove intermediate searching indicator
            }

        // A complete multi-line diagnostic buffer can contain both positive
        // evidence and responder errors. Preserve every line for the envelope
        // parser; only incomplete/transport failures invalidate the whole buffer.
        if (!commandSent.trim().startsWith("AT", true) && lines.size > 1) {
            return AdapterResponse.Success(withoutPrompt, lines, elapsedMs)
        }

        // Check for recognized ELM error messages
        val combinedUpper = withoutPrompt.uppercase()

        return when {
            combinedUpper.contains("NO DATA") -> {
                AdapterResponse.Error(AdapterError.NoData, withoutPrompt, elapsedMs)
            }
            combinedUpper.contains("UNABLE TO CONNECT") -> {
                AdapterResponse.Error(AdapterError.UnableToConnect, withoutPrompt, elapsedMs)
            }
            combinedUpper.contains("BUS INIT") && combinedUpper.contains("ERROR") -> {
                AdapterResponse.Error(AdapterError.BusInitFailed, withoutPrompt, elapsedMs)
            }
            combinedUpper.contains("BUS BUSY") -> {
                AdapterResponse.Error(AdapterError.BusBusy, withoutPrompt, elapsedMs)
            }
            combinedUpper.contains("CAN ERROR") -> {
                AdapterResponse.Error(AdapterError.CanError, withoutPrompt, elapsedMs)
            }
            combinedUpper.contains("STOPPED") -> {
                AdapterResponse.Error(AdapterError.Stopped, withoutPrompt, elapsedMs)
            }
            combinedUpper.contains("BUFFER FULL") -> {
                AdapterResponse.Error(AdapterError.BufferOverflow, withoutPrompt, elapsedMs)
            }
            combinedUpper.contains("LV RESET") -> {
                AdapterResponse.Error(AdapterError.LowVoltageReset, withoutPrompt, elapsedMs)
            }
            lines.any { it == "?" } -> {
                AdapterResponse.Error(AdapterError.UnknownCommand, withoutPrompt, elapsedMs)
            }
            else -> {
                AdapterResponse.Success(
                    rawResponse = withoutPrompt,
                    cleanedLines = lines,
                    executionTimeMs = elapsedMs
                )
            }
        }
    }
}
