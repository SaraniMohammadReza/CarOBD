package com.sarani.carobd.core.model

/** One GATT generation. A late descriptor callback cannot revive a closed link. */
class BleReadiness {
    @Volatile private var closed = false
    val isClosed: Boolean get() = closed
    private var linked = false
    private var subscriptionPending = false
    @Volatile var isReady = false
        private set
    @Synchronized fun linkConnected(): Boolean {
        if (closed) return false
        linked = true
        return true
    }
    @Synchronized fun beginSubscription(): Boolean {
        if (closed || !linked) return false
        subscriptionPending = true
        return true
    }
    @Synchronized fun acknowledgeSubscription(success: Boolean): Boolean {
        isReady = !closed && linked && subscriptionPending && success
        subscriptionPending = false
        return isReady
    }
    @Synchronized fun disconnect() {
        closed = true
        linked = false
        subscriptionPending = false
        isReady = false
    }
}
