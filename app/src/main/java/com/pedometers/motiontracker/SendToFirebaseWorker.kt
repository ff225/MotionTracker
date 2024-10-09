package com.pedometers.motiontracker

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import java.io.File

class SendToFirebaseWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private val storage = Firebase.storage.reference

    override suspend fun doWork(): Result {

        val path = inputData.getString("file")?.toUri()
        // get file from directory
        val file = path?.let { File(it.path) }
        // upload file to firebase
        storage.child(file!!.name).putFile(file.toUri())
            .addOnSuccessListener {
                Log.d("SendToFirebaseWorker", "File uploaded successfully")
                // Delete file
                file.delete()
            }
            .addOnFailureListener {
                Log.e("SendToFirebaseWorker", "Error uploading file", it)
            }
        return Result.success()
    }
}