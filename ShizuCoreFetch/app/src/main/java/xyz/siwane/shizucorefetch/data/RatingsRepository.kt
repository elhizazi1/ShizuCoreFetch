package xyz.siwane.shizucorefetch.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** ملخّص تقييم تطبيق واحد: المعدل الحقيقي من 5 نجوم، العدد الكلي، وتوزيع النجوم 1..5 */
data class AppRatingSummary(
    val average: Double = 0.0,
    val count: Int = 0,
    val distribution: Map<Int, Int> = emptyMap()
)

/**
 * قواعد Firestore (كما ذكرت أنها موجودة مسبقًا لديك) يُفترض أن تتحقق من صحة الكتابة على المستندين معًا
 * (مستند التجميع + مستند تقييم المستخدم) عبر معاملة Transaction، وأن تمنع أي مستخدم من تعديل تجميع
 * تطبيق غير مرتبط بتقييمه الخاص فقط. هذا الملف ينفّذ الجزء الخاص بالتطبيق (Client) فقط.
 *
 * بنية المجموعات في Firestore:
 *  ratings/{owner_repo}                -> { sum: Int, count: Int, average: Double, d1..d5: Int }
 *  ratings/{owner_repo}/users/{login}  -> { stars: Int, updatedAt: Timestamp }
 */
class RatingsRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private fun docId(owner: String, repo: String) = "${owner}_$repo".lowercase()

    /** يستمع لتغييرات التقييم الحقيقية لحظيًا (Snapshot Listener) */
    fun observeRating(owner: String, repo: String): Flow<AppRatingSummary> = callbackFlow {
        val ref = firestore.collection("ratings").document(docId(owner, repo))
        val registration = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(AppRatingSummary())
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) {
                trySend(AppRatingSummary())
                return@addSnapshotListener
            }
            val count = (snapshot.getLong("count") ?: 0L).toInt()
            val average = snapshot.getDouble("average") ?: 0.0
            val distribution = (1..5).associateWith { star ->
                (snapshot.getLong("d$star") ?: 0L).toInt()
            }
            trySend(AppRatingSummary(average = average, count = count, distribution = distribution))
        }
        awaitClose { registration.remove() }
    }

    /** يجلب تقييم المستخدم الحالي المخزّن سابقًا لهذا التطبيق (إن وُجد) */
    suspend fun getMyRating(owner: String, repo: String, username: String): Int? {
        if (username.isBlank()) return null
        val doc = firestore.collection("ratings").document(docId(owner, repo))
            .collection("users").document(username.lowercase())
            .get().await()
        return if (doc.exists()) (doc.getLong("stars") ?: 0L).toInt() else null
    }

    /**
     * ينشر/يحدّث تقييم المستخدم الحقيقي بمعاملة Firestore (Transaction) لضمان دقة الحساب
     * الكلي (المعدل عدد النجوم من 1 إلى 5) حتى مع تعديل مستخدمين آخرين لتقييماتهم في نفس اللحظة.
     */
    suspend fun submitRating(owner: String, repo: String, username: String, stars: Int) {
        require(stars in 1..5) { "التقييم يجب أن يكون بين 1 و5 نجوم" }
        require(username.isNotBlank()) { "يجب تسجيل الدخول لإرسال تقييم" }

        val summaryRef = firestore.collection("ratings").document(docId(owner, repo))
        val userRatingRef = summaryRef.collection("users").document(username.lowercase())

        firestore.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRatingRef)
            val previousStars = if (userSnapshot.exists()) (userSnapshot.getLong("stars") ?: 0L).toInt() else null

            val summarySnapshot = transaction.get(summaryRef)
            var sum = summarySnapshot.getLong("sum") ?: 0L
            var count = summarySnapshot.getLong("count") ?: 0L
            val distribution = (1..5).associateWith { star -> summarySnapshot.getLong("d$star") ?: 0L }.toMutableMap()

            if (previousStars != null) {
                // تحديث تقييم موجود: نزيل قيمته القديمة من المجموع أولاً
                sum -= previousStars
                distribution[previousStars] = (distribution[previousStars] ?: 1L) - 1
            } else {
                count += 1
            }
            sum += stars
            distribution[stars] = (distribution[stars] ?: 0L) + 1

            val average = if (count > 0) sum.toDouble() / count.toDouble() else 0.0

            val summaryData = mutableMapOf<String, Any>(
                "sum" to sum,
                "count" to count,
                "average" to average
            )
            distribution.forEach { (star, value) -> summaryData["d$star"] = value }

            transaction.set(summaryRef, summaryData)
            transaction.set(userRatingRef, mapOf("stars" to stars, "updatedAt" to com.google.firebase.Timestamp.now()))
            null
        }.await()
    }
}
