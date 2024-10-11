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
    val position: String,
    val activityType: String
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

enum class PositionWearable {
    WRIST,
    CHEST
}

enum class FrequencyHertz(val value: Int) {
    HZ_13(13),
    HZ_26(26),
    HZ_52(52),
    HZ_104(104),
    HZ_208(208),
    HZ_416(416),
    HZ_833(833),
    HZ_1666(1666)
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