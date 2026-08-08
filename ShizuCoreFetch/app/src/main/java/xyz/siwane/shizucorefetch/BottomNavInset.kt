package xyz.siwane.shizucorefetch

import android.view.View
import androidx.core.view.updatePadding
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * يحل هذا الكائن مشكلة القيم الثابتة المخمَّنة (80dp / 90dp / 120dp) التي كانت
 * موزّعة داخل كل واجهة على حدة لحجز مساحة أسفل المحتوى تفاديًا لتداخله مع
 * البوتوم بار. تلك القيم لا تراعي اختلاف حجم البوتوم بار الفعلي بين الشاشات
 * (اختلاف كثافة البكسل، حجم الخط، أو أشرطة النظام لدى المصنّع).
 *
 * بدلًا من ذلك: [HomeActivity] تقيس الارتفاع الحقيقي للبوتوم بار بعد رسمه على
 * الشاشة وتبلغه هنا عبر [report]، وكل واجهة تربط عنصرها القابل للتمرير عبر
 * [bind] لتحصل تلقائيًا على القيمة الصحيحة بالضبط + هامش تنفّس بسيط، مع تنظيف
 * تلقائي مرتبط بدورة حياة الواجهة (لا حاجة لإزالة يدوية في onDestroyView).
 */
object BottomNavInset {

    @Volatile
    private var heightPx: Int = 0

    private val listeners = mutableSetOf<(Int) -> Unit>()

    /** تُستدعى من HomeActivity فقط، بعد قياس الحاوية الفعلية للبوتوم بار. */
    fun report(measuredHeightPx: Int) {
        if (measuredHeightPx <= 0 || measuredHeightPx == heightPx) return
        heightPx = measuredHeightPx
        listeners.toList().forEach { it(measuredHeightPx) }
    }

    /**
     * تربط أي عنصر قابل للتمرير (NestedScrollView أو RecyclerView) بارتفاع
     * البوتوم بار الحقيقي، بحيث يُحجز أسفله دائمًا هامش يساوي بالضبط ارتفاع
     * الشريط + مسافة تنفّس بصرية بسيطة بينه وبين آخر عنصر في المحتوى.
     *
     * يجب أن يمتلك العنصر بالفعل android:clipToPadding="false" في XML.
     */
    fun bind(target: View, owner: LifecycleOwner, extraGapDp: Int = 16) {
        val extraGapPx = (extraGapDp * target.resources.displayMetrics.density).toInt()
        val listener: (Int) -> Unit = { h -> target.updatePadding(bottom = h + extraGapPx) }

        if (heightPx > 0) listener(heightPx)
        listeners.add(listener)

        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                listeners.remove(listener)
            }
        })
    }
}
