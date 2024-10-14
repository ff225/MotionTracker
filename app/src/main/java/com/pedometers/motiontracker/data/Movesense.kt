package com.pedometers.motiontracker.data

import android.util.Log

object Movesense {

    var name: String? = null
        private set
    var macAddr: String? = null
        private set

    var isConnected = false

    var position: PositionWearable? = PositionWearable.WRIST
        private set

    var frequencyHertz: FrequencyHertz = FrequencyHertz.HZ_13
        private set

    fun init(name: String, macAddr: String, isConnected: Boolean = false) {
        Log.d("Movesense", "init: $name, $macAddr")
        this.name = name
        this.macAddr = macAddr
        this.isConnected = isConnected
    }


    fun setFrequency(frequencyHertz: FrequencyHertz) {
        Log.d("Movesense", "setFrequency: $frequencyHertz")
        this.frequencyHertz = frequencyHertz
    }

    fun setPosition(position: PositionWearable) {
        Log.d("Movesense", "setPosition: $position")
        this.position = position
    }

    fun reset() {
        Log.d("Movesense", "reset")
        Log.d("Movesense", "value: $name, $macAddr")
        name = null
        macAddr = null
        isConnected = false
        position = PositionWearable.WRIST
    }
}