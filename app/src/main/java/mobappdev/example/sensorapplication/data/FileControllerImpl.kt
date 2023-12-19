package mobappdev.example.sensorapplication.data

/**
 * File: AndroidPolarController.kt
 * Purpose: Implementation of the PolarController Interface.
 *          Communicates with the polar API
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mobappdev.example.sensorapplication.domain.FileController
import java.io.File
import java.io.FileWriter
import java.io.IOException

class FileControllerImpl (
    private val context: Context
): FileController {
    private val csvFile: File by lazy {

        createCsvFile()
    }


    fun createCsvFile(): File {
        val timestamp = System.currentTimeMillis()
        val fileName = "recorded_data_$timestamp.csv"
        return File(context.filesDir, fileName)
    }
    override fun saveDataToCsv(angleList: List<Float>) {
        try {
            val writer = FileWriter(csvFile, true)

            // Add header (column labels)
            writer.append("Angle,Index\n")

            // Add recorded data
            for (i in angleList.indices) {
                writer.append("${angleList[i].toInt()},$i\n")
            }

            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Log.e("SAVED", angleList.toString() + " LIST")
    }
}