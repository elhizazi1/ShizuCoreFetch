package xyz.siwane.shizucorefetch

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build // تم إضافة هذا السطر السحري هنا
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val rootView = findViewById<View>(android.R.id.content)
        EdgeToEdgeHelper.setup(this, rootView)

        val ivSplashLogo = findViewById<ImageView>(R.id.ivSplashLogo)
        val vPulseRing1 = findViewById<View>(R.id.vPulseRing1)
        val vPulseRing2 = findViewById<View>(R.id.vPulseRing2)

        if (ivSplashLogo == null) {
            goToNextActivity()
            return
        }

        // اللون الخاص بتطبيقك
        val mainColor = Color.parseColor("#00d1b2")
        val transparentMainColor = Color.argb(26, Color.red(mainColor), Color.green(mainColor), Color.blue(mainColor))

        val pulseDrawable1 = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(transparentMainColor)
        }
        val pulseDrawable2 = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(transparentMainColor)
        }

        vPulseRing1?.background = pulseDrawable1
        vPulseRing2?.background = pulseDrawable2

        // 1. حركة ظهور الشعار (يكبر بانسيابية)
        ivSplashLogo.scaleX = 0f
        ivSplashLogo.scaleY = 0f
        ivSplashLogo.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        // 2. انطلاق حلقات التموج بعد استقرار الشعار
        vPulseRing1?.let { animatePulse(it, 800, 2000) }
        vPulseRing2?.let { animatePulse(it, 1400, 2000) }

        // 3. الانتقال للواجهة الصحيحة
        Handler(Looper.getMainLooper()).postDelayed({
            goToNextActivity()
        }, 2400)
    }

    private fun animatePulse(view: View, delay: Long, duration: Long) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 4.5f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 4.5f)
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.8f, 0f)

        val smoothInterpolator = LinearInterpolator()
        scaleX.interpolator = smoothInterpolator
        scaleY.interpolator = smoothInterpolator
        alpha.interpolator = smoothInterpolator

        scaleX.repeatCount = ObjectAnimator.INFINITE
        scaleY.repeatCount = ObjectAnimator.INFINITE
        alpha.repeatCount = ObjectAnimator.INFINITE

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.duration = duration
        animatorSet.startDelay = delay
        animatorSet.start()
    }

    private fun goToNextActivity() {
        if (!isFinishing && !isDestroyed) {
            val token = AuthManager.getToken(this)
            val nextActivity = if (!token.isNullOrEmpty()) {
                HomeActivity::class.java
            } else {
                LoginActivity::class.java
            }
            startActivity(Intent(this, nextActivity))
            
            // التعديل الخاص بك للتعامل مع الحركة في الأجهزة الحديثة والقديمة
            if (Build.VERSION.SDK_INT >= 34) {
                overrideActivityTransition(
                    android.app.Activity.OVERRIDE_TRANSITION_OPEN, 
                    android.R.anim.fade_in, 
                    android.R.anim.fade_out
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            
            finish()
        }
    }
}
