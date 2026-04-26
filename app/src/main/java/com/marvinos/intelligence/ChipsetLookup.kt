package com.marvinos.intelligence

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps raw Android SOC/BOARD string values to human-readable marketing names.
 *
 * For MVP, this uses an in-memory map.
 * In a real-world scenario (Sprint 6+), this would be backed by a remote config JSON.
 */
@Singleton
class ChipsetLookup @Inject constructor() {

    data class ChipsetTier(val name: String, val tier: String, val year: Int?)

    private val lookupTable = mapOf(
        // Snapdragon 8 series
        "lahaina" to ChipsetTier("Snapdragon 888", "flagship", 2021),
        "taro" to ChipsetTier("Snapdragon 8 Gen 1", "flagship", 2022),
        "kalama" to ChipsetTier("Snapdragon 8 Gen 2", "flagship", 2023),
        "pineapple" to ChipsetTier("Snapdragon 8 Gen 3", "flagship", 2024),
        
        // Snapdragon 7 series
        "SM7325" to ChipsetTier("Snapdragon 778G", "upper-mid", 2021),
        "ukulele" to ChipsetTier("Snapdragon 7 Gen 3", "upper-mid", 2023),
        
        // MediaTek
        "MT6769V" to ChipsetTier("MediaTek Helio G85", "entry", 2020),
        "MT6893" to ChipsetTier("MediaTek Dimensity 1200", "upper-mid", 2021),
        "MT6983" to ChipsetTier("MediaTek Dimensity 9000", "flagship", 2022),
        
        // Google Tensor
        "slider" to ChipsetTier("Google Tensor G1", "flagship", 2021),
        "cloudripper" to ChipsetTier("Google Tensor G2", "flagship", 2022),
        "zuma" to ChipsetTier("Google Tensor G3", "flagship", 2023)
    )

    /**
     * Resolves a raw board/soc model name into a tier.
     * If not found, returns a safe fallback.
     */
    fun lookup(rawIdentifier: String): ChipsetTier {
        val exactMatch = lookupTable[rawIdentifier]
        if (exactMatch != null) return exactMatch
        
        // Partial matching logic for variations
        for ((key, value) in lookupTable) {
            if (rawIdentifier.contains(key, ignoreCase = true)) {
                return value
            }
        }
        
        return ChipsetTier(rawIdentifier, "unknown", null)
    }
}
