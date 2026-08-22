package xyz.siwane.shizucorefetch.shizuku

import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/**
 * غلاف حقيقي حول Shizuku API الرسمية (dev.rikka.shizuku:api).
 * كل القيم هنا حقيقية 100%: لا محاكاة. isRunning يعتمد فعليًا على Shizuku.pingBinder()،
 * isGranted يعتمد فعليًا على Shizuku.checkSelfPermission()، وطلب الصلاحية يفتح فعليًا
 * حوار نظام Shizuku الحقيقي (أو تطبيق Shizuku Manager في الإصدارات الأقدم من binder).
 *
 * يجب استدعاء [register] مرة واحدة عند بدء التطبيق (مثلاً في Application أو MainActivity.onCreate)
 * حتى تُسجَّل المستمعات (listeners) الحقيقية لأحداث Shizuku، ويجب استدعاء [unregister] عند الإغلاق.
 */
object ShizukuManager {

    const val REQUEST_CODE = 8901

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isGranted = MutableStateFlow(false)
    val isGranted: StateFlow<Boolean> = _isGranted.asStateFlow()

    private val _binderVersion = MutableStateFlow(-1)
    val binderVersion: StateFlow<Int> = _binderVersion.asStateFlow()

    private var isRegistered = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isRunning.value = false
        _isGranted.value = false
        _binderVersion.value = -1
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE) {
            _isGranted.value = grantResult == PackageManager.PERMISSION_GRANTED
        }
    }

    fun register() {
        if (isRegistered) return
        isRegistered = true
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        refreshState()
    }

    fun unregister() {
        if (!isRegistered) return
        isRegistered = false
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    /** يعيد فحص الحالة الحقيقية للاتصال بخدمة Shizuku (زر التحديث/زر التحقق من الصلاحية) */
    fun refreshState() {
        val running = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
        _isRunning.value = running

        if (!running) {
            _isGranted.value = false
            _binderVersion.value = -1
            return
        }

        _binderVersion.value = try {
            Shizuku.getVersion()
        } catch (_: Throwable) {
            -1
        }

        _isGranted.value = try {
            if (Shizuku.isPreV11()) {
                // إصدارات binder الأقدم من 11 تتطلب Root، ولا تدعم checkSelfPermission بنفس الشكل
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (_: Throwable) {
            false
        }
    }

    /** يطلب صلاحية Shizuku الحقيقية من المستخدم (حوار نظام حقيقي) */
    fun requestPermission() {
        if (!_isRunning.value) return
        if (_isGranted.value) return
        try {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                // المستخدم رفض سابقًا برفض دائم؛ التطبيق يعرض توضيحًا في الواجهة (يُعالَج في الشاشة)
            }
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (_: Throwable) {
            // فشل غير متوقع (مثلاً الخدمة توقفت للتو)؛ إعادة فحص الحالة
            refreshState()
        }
    }
}
