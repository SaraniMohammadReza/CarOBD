package com.sarani.carobd.core.telemetry.buffer

/**
 * High-performance, bounded circular buffer for real-time sensor telemetry and graphs.
 * Fixed capacity with zero reallocation during runtime.
 */
class BoundedRingBuffer(val capacity: Int = 120) {

    private val buffer = FloatArray(capacity)
    private var head = 0
    private var count = 0

    val size: Int
        get() = count

    val isFull: Boolean
        get() = count == capacity

    val isEmpty: Boolean
        get() = count == 0

    /**
     * Appends a new sample to the circular buffer.
     * Overwrites oldest sample if capacity is exceeded.
     */
    @Synchronized
    fun add(value: Float) {
        buffer[head] = value
        head = (head + 1) % capacity
        if (count < capacity) {
            count++
        }
    }

    /**
     * Clears all samples.
     */
    @Synchronized
    fun clear() {
        head = 0
        count = 0
    }

    /**
     * Returns the latest inserted value, or null if buffer is empty.
     */
    @Synchronized
    fun latest(): Float? {
        if (count == 0) return null
        val lastIndex = (head - 1 + capacity) % capacity
        return buffer[lastIndex]
    }

    /**
     * Returns all samples in chronological order (oldest to newest).
     */
    @Synchronized
    fun toList(): List<Float> {
        val result = ArrayList<Float>(count)
        val start = if (count == capacity) head else 0
        for (i in 0 until count) {
            val idx = (start + i) % capacity
            result.add(buffer[idx])
        }
        return result
    }

    /**
     * Returns statistical analysis of the stored samples.
     */
    @Synchronized
    fun getStatistics(): BufferStatistics {
        if (count == 0) return BufferStatistics(0f, 0f, 0f, 0f, 0f)

        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        var sum = 0.0

        val start = if (count == capacity) head else 0
        for (i in 0 until count) {
            val v = buffer[(start + i) % capacity]
            if (v < min) min = v
            if (v > max) max = v
            sum += v
        }

        val avg = (sum / count).toFloat()
        val latestVal = buffer[(head - 1 + capacity) % capacity]
        val oldestVal = buffer[start]
        val delta = latestVal - oldestVal

        return BufferStatistics(
            min = min,
            max = max,
            average = avg,
            latest = latestVal,
            delta = delta
        )
    }
}

data class BufferStatistics(
    val min: Float,
    val max: Float,
    val average: Float,
    val latest: Float,
    val delta: Float
)
