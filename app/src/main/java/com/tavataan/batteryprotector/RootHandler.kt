package com.tavataan.bypass

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object RootHandler {
    
    fun isRootGranted(): Boolean {
    
        val output = readSafeSuCommand("id")
        return output?.contains("uid=0") == true
    }
    
    private const val PATH_FG_EXACTO = "/sys/class/power_supply/battery/fg_fullcapnom"
    private const val PATH_CHARGE_FULL = "/sys/class/power_supply/battery/charge_full"
    private const val PATH_DESIGN = "/sys/class/power_supply/battery/charge_full_design"
    private const val CURRENT_NOW_PATH = "/sys/class/power_supply/battery/batt_current_ua_now"
    
    private const val TEST_MODE_PATH = "/sys/class/power_supply/battery/test_mode"

    fun getBatteryHealthData(): Pair<Int, Int>? {
        var currentRawStr = readSafeSuCommand("cat $PATH_FG_EXACTO")
        
        if (currentRawStr == null || currentRawStr.trim() == "" || currentRawStr == "0") {
            currentRawStr = readSafeSuCommand("cat $PATH_CHARGE_FULL")
        }

        val designRawStr = readSafeSuCommand("cat $PATH_DESIGN")

        if (currentRawStr == null || designRawStr == null) return null

        val currentRaw = currentRawStr.toDoubleOrNull() ?: return null
        val designRaw = designRawStr.toDoubleOrNull() ?: return null

        if (designRaw == 0.0) return null

        val currentMah = if (currentRaw > 100000) currentRaw / 1000.0 else currentRaw
        val designMah = if (designRaw > 100000) designRaw / 1000.0 else designRaw

        val healthPercentage = ((currentMah / designMah) * 100).toInt()
        val currentCapacityDisplay = currentMah.toInt()

        return Pair(healthPercentage, currentCapacityDisplay)
    }

    fun getBatteryCurrentNow(): String? {
        val rawValue = readSafeSuCommand("cat $CURRENT_NOW_PATH") ?: return null
        val microAmps = rawValue.toLongOrNull() ?: return null
        return "${microAmps / 1000} mA"
    }

    fun setBypass(enable: Boolean): Boolean {
       
        val valTest = if (enable) "1" else "0"
        
        val success = executeSafeSuCommand("echo $valTest > $TEST_MODE_PATH")

        val checkTest = readSafeSuCommand("cat $TEST_MODE_PATH")
        
        return success && (checkTest == valTest)
    }
    fun isBypassActive(): Boolean {
        val checkTest = readSafeSuCommand("cat $TEST_MODE_PATH")
        return checkTest == "1"
    }
    private fun executeSafeSuCommand(command: String): Boolean {
        return try {
            val process = ProcessBuilder("su", "-mm", "-c", command)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(3000, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroy()
                return false
            }
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun readSafeSuCommand(command: String): String? {
        return try {
            val process = ProcessBuilder("su", "-mm", "-c", command)
                .redirectErrorStream(true)
                .start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            val finished = process.waitFor(1500, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroy()
                return null
            }
            sb.toString().trim()
        } catch (e: Exception) {
            null
        }
    }
}