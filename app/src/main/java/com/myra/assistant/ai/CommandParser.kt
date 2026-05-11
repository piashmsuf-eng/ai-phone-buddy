package com.myra.assistant.ai

import android.util.Log
import com.myra.assistant.model.AppCommand

object CommandParser {
    private val appMap = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "chrome" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "spotify" to "com.spotify.music",
        "netflix" to "com.netflix.mediaclient",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "telegram" to "org.telegram.messenger",
        "snapchat" to "com.snapchat.android",
        "settings" to "com.android.settings",
        "calculator" to "com.android.calculator2",
        "calendar" to "com.google.android.calendar",
        "clock" to "com.android.deskclock",
        "phone" to "com.android.dialer",
        "contacts" to "com.android.contacts",
        "play store" to "com.android.vending",
        "amazon" to "com.amazon.mShop.android.shopping",
        "flipkart" to "com.flipkart.android",
        "paytm" to "net.one97.paytm",
        "phonepe" to "com.phonepe.app",
        "gpay" to "com.google.android.apps.nbu.paisa.user",
        "zoom" to "us.zoom.videomeetings",
        "meet" to "com.google.android.apps.meetings",
        "teams" to "com.microsoft.teams",
        "tiktok" to "com.zhiliaoapp.musically",
        "discord" to "com.discord",
        "linkedin" to "com.linkedin.android",
        "imdb" to "com.imdb.mobile"
    )

    fun parse(inputRaw: String): AppCommand? {
        val input = inputRaw.lowercase().trim()
        Log.d("CommandParser", "Parsing: $input")

        for ((name, pkg) in appMap) {
            if (input.contains("open $name") || input.contains("$name kholo") || input.contains("$name chalao") || input.contains("$name open")) {
                return AppCommand("open_app", mapOf("package" to pkg, "name" to name))
            }
            if (input.contains("close $name") || input.contains("$name band karo") || input.contains("$name band")) {
                return AppCommand("close_app", mapOf("name" to name))
            }
        }

        if (Regex("call (.+)").containsMatchIn(input) ||
            Regex("(.+) ko call karo").containsMatchIn(input) ||
            Regex("(.+) ko call").containsMatchIn(input)) {
            val name = extractName(input, "call")
            if (name != null) return AppCommand("call", mapOf("name" to name))
        }

        if (input.contains("volume badhao") || input.contains("volume up") || input.contains("volume increase") || input.contains("vol up")) {
            return AppCommand("volume_up", emptyMap())
        }
        if (input.contains("volume kam") || input.contains("volume down") || input.contains("volume decrease") || input.contains("vol down")) {
            return AppCommand("volume_down", emptyMap())
        }
        if (input.contains("torch on") || input.contains("flashlight on") || input.contains("flash on")) {
            return AppCommand("flashlight_on", emptyMap())
        }
        if (input.contains("torch off") || input.contains("flashlight off") || input.contains("flash off")) {
            return AppCommand("flashlight_off", emptyMap())
        }
        if (input.contains("wifi on")) return AppCommand("wifi_on", emptyMap())
        if (input.contains("wifi off")) return AppCommand("wifi_off", emptyMap())
        if (input.contains("bluetooth on") || input.contains("bluetooth chalao")) return AppCommand("bluetooth_on", emptyMap())
        if (input.contains("bluetooth off") || input.contains("bluetooth band")) return AppCommand("bluetooth_off", emptyMap())

        if (input.contains("my close friend") || input.contains("my friend") || input.contains("mere close friend") || input.contains("prime contact")) {
            if (input.contains("call") || input.contains("call karo")) {
                return AppCommand("prime_call", mapOf("index" to "0"))
            }
            if (input.contains("message") || input.contains("msg") || input.contains("sms")) {
                val msg = input.substringAfter("message").substringAfter("msg").trim().ifBlank { null }
                return AppCommand("prime_msg", mapOf("index" to "0", "message" to (msg ?: "")))
            }
        }

        return null
    }

    private fun extractName(input: String, prefix: String): String? {
        val patterns = listOf(
            Regex("$prefix\\s+(.+)"),
            Regex("(.+)\\s+ko\\s+$prefix\\s+karo"),
            Regex("(.+)\\s+ko\\s+$prefix")
        )
        for (p in patterns) {
            val match = p.find(input) ?: continue
            val name = match.groupValues[1].trim().replace("ko", "").trim()
            if (name.isNotBlank() && name.length < 30) return name
        }
        return null
    }
}
