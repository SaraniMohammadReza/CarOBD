package com.sarani.carobd.core.protocol.isotp

/**
 * ISO 15765-2 (ISO-TP) Network Layer Protocol Data Unit (N_PDU) types.
 */
enum class IsoTpFrameType(val pciType: Int) {
    SINGLE_FRAME(0x0),
    FIRST_FRAME(0x1),
    CONSECUTIVE_FRAME(0x2),
    FLOW_CONTROL(0x3);

    companion object {
        fun fromPciByte(pci: Int): IsoTpFrameType? {
            val typeNibble = (pci ushr 4) and 0x0F
            return entries.firstOrNull { it.pciType == typeNibble }
        }
    }
}

/**
 * Flow Status values for ISO-TP Flow Control frames (PCI type 3).
 */
enum class FlowStatus(val code: Int) {
    CONTINUE_TO_SEND(0x0),
    WAIT(0x1),
    OVERFLOW(0x2);

    companion object {
        fun fromCode(code: Int): FlowStatus = entries.firstOrNull { it.code == code } ?: OVERFLOW
    }
}

/**
 * Parsed ISO-TP CAN Frame representation.
 */
sealed class IsoTpFrame {
    data class SingleFrame(
        val dataLength: Int,
        val payload: ByteArray
    ) : IsoTpFrame() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SingleFrame
            return dataLength == other.dataLength && payload.contentEquals(other.payload)
        }
        override fun hashCode(): Int = 31 * dataLength + payload.contentHashCode()
    }

    data class FirstFrame(
        val totalLength: Int,
        val initialPayload: ByteArray
    ) : IsoTpFrame() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as FirstFrame
            return totalLength == other.totalLength && initialPayload.contentEquals(other.initialPayload)
        }
        override fun hashCode(): Int = 31 * totalLength + initialPayload.contentHashCode()
    }

    data class ConsecutiveFrame(
        val sequenceNumber: Int,
        val chunk: ByteArray
    ) : IsoTpFrame() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ConsecutiveFrame
            return sequenceNumber == other.sequenceNumber && chunk.contentEquals(other.chunk)
        }
        override fun hashCode(): Int = 31 * sequenceNumber + chunk.contentHashCode()
    }

    data class FlowControl(
        val flowStatus: FlowStatus,
        val blockSize: Int,
        val separationTimeMs: Int
    ) : IsoTpFrame()
}

/**
 * ISO 15765-2 Multi-Frame Reassembler.
 * Handles stateful reassembly of segmented CAN frames into complete diagnostic messages.
 */
class IsoTpReassembler {

    private var expectedTotalLength = 0
    private var nextExpectedSeqNumber = 1
    private val buffer = mutableListOf<Byte>()
    private var isAssembling = false

    val isComplete: Boolean
        get() = isAssembling && buffer.size >= expectedTotalLength

    val progress: Float
        get() = if (expectedTotalLength == 0) 0f else (buffer.size.toFloat() / expectedTotalLength).coerceIn(0f, 1f)

    /**
     * Resets the reassembly buffer and state.
     */
    fun reset() {
        buffer.clear()
        expectedTotalLength = 0
        nextExpectedSeqNumber = 1
        isAssembling = false
    }

    /**
     * Feeds a raw 8-byte CAN frame payload into the ISO-TP state machine.
     * Returns the fully reassembled payload if this frame completed the message, or null if more frames are expected.
     */
    fun processFrame(rawBytes: ByteArray): ByteArray? {
        if (rawBytes.isEmpty()) return null

        val pciByte = rawBytes[0].toInt() and 0xFF
        val frameType = IsoTpFrameType.fromPciByte(pciByte) ?: return null

        when (frameType) {
            IsoTpFrameType.SINGLE_FRAME -> {
                reset()
                val length = pciByte and 0x0F
                if (length == 0 || length > rawBytes.size - 1) return null
                return rawBytes.copyOfRange(1, 1 + length)
            }

            IsoTpFrameType.FIRST_FRAME -> {
                reset()
                if (rawBytes.size < 2) return null
                val secondByte = rawBytes[1].toInt() and 0xFF
                expectedTotalLength = ((pciByte and 0x0F) shl 8) or secondByte
                
                val initialBytes = rawBytes.copyOfRange(2, rawBytes.size)
                buffer.addAll(initialBytes.toList())
                nextExpectedSeqNumber = 1
                isAssembling = true

                return if (buffer.size >= expectedTotalLength) {
                    val result = buffer.take(expectedTotalLength).toByteArray()
                    reset()
                    result
                } else {
                    null
                }
            }

            IsoTpFrameType.CONSECUTIVE_FRAME -> {
                if (!isAssembling) return null
                val seq = pciByte and 0x0F
                if (seq != nextExpectedSeqNumber) {
                    // Sequence error: frame drop or out of order
                    reset()
                    return null
                }

                nextExpectedSeqNumber = (nextExpectedSeqNumber + 1) and 0x0F
                val remainingNeeded = expectedTotalLength - buffer.size
                val payloadInFrame = rawBytes.size - 1
                val bytesToTake = minOf(payloadInFrame, remainingNeeded)

                for (i in 1..bytesToTake) {
                    buffer.add(rawBytes[i])
                }

                return if (buffer.size >= expectedTotalLength) {
                    val result = buffer.take(expectedTotalLength).toByteArray()
                    reset()
                    result
                } else {
                    null
                }
            }

            IsoTpFrameType.FLOW_CONTROL -> {
                // Handled by sender, not by reassembler
                return null
            }
        }
    }

    /**
     * Segment an outgoing payload into ISO-TP CAN frames according to ISO 15765-2.
     */
    companion object {
        fun segmentPayload(payload: ByteArray, maxFrameSize: Int = 8): List<ByteArray> {
            val frames = mutableListOf<ByteArray>()
            val length = payload.size

            if (length <= maxFrameSize - 1) {
                // Single Frame fits in 1..7 data bytes (with 1 byte PCI)
                val frame = ByteArray(length + 1)
                frame[0] = length.toByte() // SF PCI: 0x00 | length
                System.arraycopy(payload, 0, frame, 1, length)
                frames.add(padFrame(frame, maxFrameSize))
                return frames
            }

            // Multi-frame: First Frame (FF) + Consecutive Frames (CF)
            val ff = ByteArray(maxFrameSize)
            ff[0] = (0x10 or ((length ushr 8) and 0x0F)).toByte()
            ff[1] = (length and 0xFF).toByte()
            val ffDataLen = maxFrameSize - 2
            System.arraycopy(payload, 0, ff, 2, ffDataLen)
            frames.add(ff)

            var offset = ffDataLen
            var seqNum = 1

            while (offset < length) {
                val cf = ByteArray(maxFrameSize)
                cf[0] = (0x20 or (seqNum and 0x0F)).toByte()
                val chunkLen = minOf(length - offset, maxFrameSize - 1)
                System.arraycopy(payload, offset, cf, 1, chunkLen)
                if (chunkLen < maxFrameSize - 1) {
                    // Pad unused bytes with 0xAA or 0x00 per standard CAN padding
                    for (p in (chunkLen + 1) until maxFrameSize) {
                        cf[p] = 0xAA.toByte()
                    }
                }
                frames.add(cf)
                offset += chunkLen
                seqNum = (seqNum + 1) and 0x0F
            }

            return frames
        }

        fun createFlowControlFrame(
            status: FlowStatus = FlowStatus.CONTINUE_TO_SEND,
            blockSize: Int = 0,
            separationTimeMs: Int = 0
        ): ByteArray {
            return byteArrayOf(
                (0x30 or (status.code and 0x0F)).toByte(),
                blockSize.toByte(),
                separationTimeMs.coerceIn(0, 127).toByte(),
                0xAA.toByte(), 0xAA.toByte(), 0xAA.toByte(), 0xAA.toByte(), 0xAA.toByte()
            )
        }

        private fun padFrame(frame: ByteArray, targetSize: Int): ByteArray {
            if (frame.size >= targetSize) return frame
            val padded = ByteArray(targetSize) { 0xAA.toByte() }
            System.arraycopy(frame, 0, padded, 0, frame.size)
            return padded
        }
    }
}
