package com.marvinos.intelligence

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the game compatibility database.
 * For MVP, this is a hardcoded JSON string representing what would be loaded from assets.
 */
@Singleton
class GameCompatChecker @Inject constructor() {

    fun getGameDatabaseJson(): String {
        return """
        {
          "games": {
            "call of duty mobile": {
              "minRamGB": 2, "recRamGB": 4, "minGles": "3.1", "minAndroid": 5.1,
              "tierRating": { "flagship": "excellent", "upper-mid": "good", "mid": "ok-low-settings", "entry": "struggles" }
            },
            "pubg mobile": {
              "minRamGB": 2, "recRamGB": 4, "minGles": "3.1", "minAndroid": 5.1,
              "tierRating": { "flagship": "excellent", "upper-mid": "good", "mid": "ok-low-settings", "entry": "struggles" }
            },
            "genshin impact": {
              "minRamGB": 4, "recRamGB": 8, "minGles": "3.2", "minAndroid": 8.0,
              "tierRating": { "flagship": "good", "upper-mid": "ok-medium-settings", "mid": "struggles", "entry": "unplayable" }
            },
            "free fire": {
              "minRamGB": 1, "recRamGB": 2, "minGles": "2.0", "minAndroid": 4.1,
              "tierRating": { "flagship": "excellent", "upper-mid": "excellent", "mid": "good", "entry": "playable" }
            },
            "asphalt 9": {
              "minRamGB": 2, "recRamGB": 4, "minGles": "3.0", "minAndroid": 7.0,
              "tierRating": { "flagship": "excellent", "upper-mid": "good", "mid": "ok-low-settings", "entry": "struggles" }
            }
          }
        }
        """.trimIndent()
    }
}
