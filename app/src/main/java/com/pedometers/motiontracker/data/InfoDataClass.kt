package com.pedometers.motiontracker.data

/**
 * Data class to store information about the user
 *
 */
data class InfoDataClass(
    val sex: String,
    val age: Int,
    val height: Int,
    val weight: Int,
    val position: String
)

enum class Sex {
    MALE,
    FEMALE
}

/**
 * Enum class to store the position of the phone
 *
 */
enum class Position {
    FOREARM,
    POCKET,
    BELT
}

/**
 * Enum class to store the type of activity
 *
 */
enum class ActivityType {
    SLOW_WALKING,
    FAST_WALKING,
    RUNNING
}