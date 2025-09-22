package com.pedometers.motiontracker

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SendToFirebaseWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private val storage = Firebase.storage.reference

    override suspend fun doWork(): Result {
        return try {
            val filePath = inputData.getString("file")
            if (filePath.isNullOrBlank()) {
                Log.e("SendToFirebaseWorker", "No file path provided")
                return Result.failure()
            }

            val file = File(filePath)
            if (!file.exists()) {
                Log.e("SendToFirebaseWorker", "File does not exist: $filePath")
                return Result.failure()
            }

            if (file.length() == 0L) {
                Log.w("SendToFirebaseWorker", "File is empty, deleting: $filePath")
                file.delete()
                return Result.success()
            }

            Log.d("SendToFirebaseWorker", "Starting upload for file: ${file.name} (${file.length()} bytes)")

            // Generate current date folder path (yyyy-MM-dd format)
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormatter.format(Date())
            val firebasePath = "motion_data/$currentDate/${file.name}"
            
            Log.d("SendToFirebaseWorker", "Uploading to Firebase path: $firebasePath")

            // Use suspendCoroutine to properly handle the async Firebase upload
            val uploadResult = suspendCoroutine { continuation ->
                storage.child(firebasePath)
                    .putFile(file.toUri())
                    .addOnSuccessListener { taskSnapshot ->
                        Log.d("SendToFirebaseWorker", "File uploaded successfully: ${file.name}")
                        Log.d("SendToFirebaseWorker", "Upload path: $firebasePath")
                        Log.d("SendToFirebaseWorker", "Upload metadata: ${taskSnapshot.metadata?.path}")
                        continuation.resume(true)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("SendToFirebaseWorker", "Error uploading file: ${file.name}", exception)
                        continuation.resume(false)
                    }
            }

            if (uploadResult) {
                // Only delete file after successful upload
                try {
                    if (file.delete()) {
                        Log.d("SendToFirebaseWorker", "Local file deleted after successful upload: ${file.name}")
                    } else {
                        Log.w("SendToFirebaseWorker", "Could not delete local file: ${file.name}")
                    }
                } catch (e: Exception) {
                    Log.e("SendToFirebaseWorker", "Error deleting local file: ${e.message}")
                }
                Result.success()
            } else {
                Log.e("SendToFirebaseWorker", "Upload failed for file: ${file.name}")
                Result.retry() // Retry the upload
            }

        } catch (e: Exception) {
            Log.e("SendToFirebaseWorker", "Unexpected error in file upload", e)
            Result.failure()
        }
    }
}