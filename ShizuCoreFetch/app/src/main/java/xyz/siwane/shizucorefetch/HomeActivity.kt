package xyz.siwane.shizucorefetch

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder // تم إضافة هذا الاستيراد للرسالة
import xyz.siwane.shizucorefetch.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // الصلاحية مُنحت أو رُفضت، لن نقوم بإزعاج المستخدم هنا
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        EdgeToEdgeHelper.setup(this, binding.root)
        
        setupSolidBackgroundAndInsets()

        // استدعاء رسالة التنبيه الجوهرية (ستظهر لمرة واحدة فقط)
        showWelcomeNoticeOnce()

        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), R.id.nav_home)
        }

        setupNavigation()
        
        binding.bottomNavigation.post {
            disableInternalClipping()
        }

        // تأخير طلب صلاحية الإشعارات لمدة 1000 مللي ثانية (ثانية واحدة)
        // لضمان استقرار الواجهة ورسم جميع العناصر بسلاسة تامة
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isDestroyed && !isFinishing) {
                askNotificationPermission()
            }
        }, 1000)
    }

    // الدالة الجديدة الخاصة برسالة التنبيه
    private fun showWelcomeNoticeOnce() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val hasShownNotice = prefs.getBoolean("has_shown_notice", false)

        if (!hasShownNotice) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.notice_title))
                .setMessage(getString(R.string.notice_message))
                .setPositiveButton(getString(R.string.notice_button)) { _, _ ->
                    // وضع علامة بأن المستخدم قد قرأ الرسالة لكي لا تزعجه مجدداً
                    prefs.edit().putBoolean("has_shown_notice", true).apply()
                }
                .setCancelable(false) // منع الإغلاق العشوائي
                .show()
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun disableInternalClipping() {
        val menuView = binding.bottomNavigation.getChildAt(0) as? ViewGroup
        menuView?.clipChildren = false
        menuView?.clipToPadding = false

        for (i in 0 until (menuView?.childCount ?: 0)) {
            val itemView = menuView?.getChildAt(i) as? ViewGroup
            itemView?.clipChildren = false
            itemView?.clipToPadding = false
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // تعديل الاهتزاز ليصبح "نقرة لوحة مفاتيح" خفيفة وأنيقة بدلاً من الاهتزاز القوي
            binding.bottomNavigation.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            animateNavigationItem(item.itemId)

            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment(), item.itemId)
                R.id.nav_store -> loadFragment(StoreFragment(), item.itemId)
                R.id.nav_account -> loadFragment(AccountFragment(), item.itemId)
                R.id.nav_settings -> loadFragment(SettingsFragment(), item.itemId)
                else -> false
            }
            true
        }
    }

    private fun animateNavigationItem(itemId: Int) {
        val menu = binding.bottomNavigation.menu
        val density = resources.displayMetrics.density
        val jumpDistance = 6f * density 
        
        for (i in 0 until menu.size()) {
            val id = menu.getItem(i).itemId
            val itemView = binding.bottomNavigation.findViewById<View>(id)
            
            val iconView = itemView?.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_icon_view)
            val labelsGroup = itemView?.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_labels_group)
            val indicatorView = itemView?.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_active_indicator_view)

            iconView?.animate()?.translationY(0f)?.setDuration(180)?.start()
            labelsGroup?.animate()?.translationY(0f)?.setDuration(180)?.start()
            indicatorView?.animate()?.translationY(0f)?.setDuration(180)?.start()
        }

        val selectedItemView = binding.bottomNavigation.findViewById<View>(itemId)
        val selectedIconView = selectedItemView?.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_icon_view)
        val selectedLabelsGroup = selectedItemView?.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_labels_group)
        val selectedIndicatorView = selectedItemView?.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_active_indicator_view)

        selectedIconView?.animate()
            ?.translationY(-jumpDistance) 
            ?.setDuration(220)
            ?.setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            ?.start()

        selectedIndicatorView?.animate()
            ?.translationY(-jumpDistance) 
            ?.setDuration(220)
            ?.setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            ?.start()

        selectedLabelsGroup?.animate()
            ?.translationY(jumpDistance) 
            ?.setDuration(220)
            ?.setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            ?.start()
    }

    private fun setupSolidBackgroundAndInsets() {
        val surfaceColor = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurface)
        
        val solidColor = Color.argb(
            255, 
            Color.red(surfaceColor),
            Color.green(surfaceColor),
            Color.blue(surfaceColor)
        )
        
        binding.bottomNavContainer.setCardBackgroundColor(solidColor)

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                val density = resources.displayMetrics.density
                val baseMargin = (-4 * density).toInt() 
                bottomMargin = baseMargin + insets.bottom
            }
            
            windowInsets
        }
    }

    private fun loadFragment(fragment: Fragment, itemId: Int): Boolean {
        animateNavigationItem(itemId)
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .commit()
        return true
    }
}
