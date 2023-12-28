package com.waywardTeam.wayward.utilities

import android.content.Context
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@Suppress("SameParameterValue")
class FileManager(private val context: Context) {
    private val userPreferencesName = "user_preferences.json"
    fun readPreferences(): UserPreferences? {
        val jsonString = readFromFile(userPreferencesName)
        if (jsonString == null) {
            savePreferences(UserPreferences(5))
        }
        return Gson().fromJson(jsonString, UserPreferences::class.java)
    }

    fun savePreferences(userPreferences: UserPreferences) {
        val json = Gson().toJson(userPreferences)
        writeToFile(userPreferencesName, json)
    }

    // function for writing data into a file
    private fun writeToFile(fileName: String, data: String?) {
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

    // function for reading data from a file
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