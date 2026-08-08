package xyz.siwane.shizucorefetch

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object RatingManager {
    private val db = FirebaseFirestore.getInstance()

    data class AppRatingStats(
        val averageRating: Float = 0f,
        val totalRatings: Long = 0
    )

    suspend fun getAppRatingStats(appId: String): AppRatingStats {
        return try {
            val doc = db.collection("apps").document(appId).get().await()
            if (doc.exists()) {
                val avg = doc.getDouble("averageRating")?.toFloat() ?: 0f
                val total = doc.getLong("totalRatings") ?: 0L
                AppRatingStats(avg, total)
            } else {
                AppRatingStats()
            }
        } catch (e: Exception) {
            AppRatingStats()
        }
    }

    suspend fun getCurrentUserRating(context: Context, appId: String): Float? {
        // الاعتماد على حساب GitHub الخاص بك بدلاً من فايربيس
        val userId = AuthManager.getUsername(context)
        if (userId.isNullOrEmpty()) return null
        
        return try {
            val docId = "${appId}_${userId}"
            val doc = db.collection("user_ratings").document(docId).get().await()
            if (doc.exists()) {
                doc.getDouble("ratingValue")?.toFloat()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun submitRating(context: Context, appId: String, newRating: Float): Boolean {
        // جلب اسم مستخدم GitHub
        val userId = AuthManager.getUsername(context)
        if (userId.isNullOrEmpty()) return false
        
        val userRatingDocId = "${appId}_${userId}"
        
        val appRef = db.collection("apps").document(appId)
        val userRatingRef = db.collection("user_ratings").document(userRatingDocId)

        return try {
            db.runTransaction { transaction ->
                val appSnapshot = transaction.get(appRef)
                val userRatingSnapshot = transaction.get(userRatingRef)

                var totalRatings = 0L
                var sumOfRatings = 0.0

                if (appSnapshot.exists()) {
                    totalRatings = appSnapshot.getLong("totalRatings") ?: 0L
                    sumOfRatings = appSnapshot.getDouble("sumOfRatings") ?: 0.0
                }

                if (userRatingSnapshot.exists()) {
                    val oldRating = userRatingSnapshot.getDouble("ratingValue") ?: 0.0
                    sumOfRatings = sumOfRatings - oldRating + newRating
                } else {
                    totalRatings += 1
                    sumOfRatings += newRating
                }

                val averageRating = if (totalRatings > 0) sumOfRatings / totalRatings else 0.0

                val appData = hashMapOf(
                    "averageRating" to averageRating,
                    "totalRatings" to totalRatings,
                    "sumOfRatings" to sumOfRatings
                )
                transaction.set(appRef, appData, SetOptions.merge())

                val userRatingData = hashMapOf(
                    "appId" to appId,
                    "userId" to userId,
                    "ratingValue" to newRating,
                    "timestamp" to System.currentTimeMillis()
                )
                transaction.set(userRatingRef, userRatingData, SetOptions.merge())
                
                null 
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
