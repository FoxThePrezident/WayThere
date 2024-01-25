package com.wayThereTeam.wayThere.utilities

import android.content.Context
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate

/**
 * Class for managing files
 * @property userSettingsName defines name of the file that is loaded
 * @property context from an activity currently working from
 */
@Suppress("SameParameterValue")
class FileManager(private val context: Context) {
    private val userSettingsName = "user_preferences.json"

    /**
     * Reads file from a storage, in case of that file is not existing, it will create a new one
     * @return data class UserSettings
     */
    fun readPreferences(): UserSettings? {
        val jsonString = readFromFile(userSettingsName)
        if (jsonString == null) {
            // If a file doesn't exist or there's an issue, create a new one with default settings
            savePreferences(UserSettings(5, emptyList(), LocalDate.now().minusDays(1)))
            // Read again after creating the file
            return readFromFile(userSettingsName)?.let {
                Gson().fromJson(it, UserSettings::class.java)
            }
        }
        return Gson().fromJson(jsonString, UserSettings::class.java)
    }

    /**
     * Save user preferences to a storage
     * @param userSettings data class that will be converted to json and saved
     */
    fun savePreferences(userSettings: UserSettings) {
        val json = Gson().toJson(userSettings)
        writeToFile(userSettingsName, json)
    }

    /**
     * Writing data into a file
     */
    private fun writeToFile(fileName: String, data: String) {
        try {
            val fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            val outputStreamWriter = OutputStreamWriter(fileOutputStream)
            // writing data
            outputStreamWriter.write(data)
            outputStreamWriter.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reading data from a file\
     * @param fileName name of file that will read from
     * @return String containing data from a file
     */
    private fun readFromFile(fileName: String): String? {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            try {
                val fileInputStream = context.openFileInput(fileName)
                val inputStreamReader = InputStreamReader(fileInputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                val stringBuilder = StringBuilder()
                var text: String?

                // reading data line by line
                while (bufferedReader.readLine().also { text = it } != null) {
                    stringBuilder.append(text)
                }
                fileInputStream.close()
                return stringBuilder.toString()
            } catch (e: Exception) {
//                return e.toString()
                e.printStackTrace()
            }
        }
        return null
    }
}