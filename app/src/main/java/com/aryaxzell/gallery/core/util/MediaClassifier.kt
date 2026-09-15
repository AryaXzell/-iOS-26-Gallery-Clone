package com.aryaxzell.gallery.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import com.aryaxzell.gallery.core.data.MediaItem
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ClassificationResult(
    val labels: List<String>,
    val belongsToPeople: Boolean,
    val belongsToPlaces: Boolean,
    val belongsToPets: Boolean
)

object MediaClassifier {
    private const val TAG = "MediaClassifier"

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.4f)
            .build()
    )

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: RuntimeException("Task failed"))
            }
        }
    }

    suspend fun classify(context: Context, item: MediaItem): ClassificationResult = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(item.uri)
                .allowHardware(false)
                .size(300, 300)
                .build()

            val result = loader.execute(request)
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val labels = labeler.process(inputImage).awaitTask()

                val labelTexts = labels.map { it.text.lowercase() }
                Log.d(TAG, "Item ${item.id} labeled successfully: $labelTexts")

                val peopleLabels = setOf(
                    "person", "human", "face", "man", "woman", "child", "girl", "boy", 
                    "crowd", "people", "portrait", "selfie", "smile", "baby", "friend", "player"
                )

                val petLabels = setOf(
                    "dog", "cat", "pet", "animal", "bird", "whiskers", "canidae", "felidae", 
                    "puppy", "kitten", "mammal", "vertebrate", "fur", "paw"
                )

                val placeLabels = setOf(
                    "mountain", "beach", "sky", "landmark", "tower", "nature", "cityscape", "street", 
                    "park", "building", "ocean", "lake", "river", "forest", "tree", "structure", 
                    "castle", "house", "room", "indoor", "outdoor", "landscape", "coast", "sand",
                    "cloud", "water", "horizon", "city", "architecture", "monument", "sea"
                )

                val belongsToPeople = labelTexts.any { label -> 
                    peopleLabels.contains(label) || label.contains("person") || label.contains("human") || label.contains("face")
                }
                val belongsToPets = labelTexts.any { label -> 
                    petLabels.contains(label) || label.contains("dog") || label.contains("cat") || label.contains("pet") || label.contains("animal")
                }
                val belongsToPlaces = labelTexts.any { label -> 
                    placeLabels.contains(label) || label.contains("mountain") || label.contains("beach") || label.contains("lake") || label.contains("forest") || label.contains("building")
                }

                ClassificationResult(
                    labels = labels.map { "${it.text} (${String.format("%.2f", it.confidence)})" },
                    belongsToPeople = belongsToPeople,
                    belongsToPlaces = belongsToPlaces,
                    belongsToPets = belongsToPets
                )
            } else {
                Log.e(TAG, "Bitmap was null for item ${item.id}")
                ClassificationResult(emptyList(), false, false, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error classifying item ${item.id}", e)
            ClassificationResult(emptyList(), false, false, false)
        }
    }
}
