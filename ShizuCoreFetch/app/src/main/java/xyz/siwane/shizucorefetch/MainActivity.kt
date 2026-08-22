package xyz.siwane.shizucorefetch

import android.Manifest
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import java.util.Locale
import xyz.siwane.shizucorefetch.notifications.NotificationHelper
import xyz.siwane.shizucorefetch.shizuku.ShizukuManager
import xyz.siwane.shizucorefetch.ui.MainScreen
import xyz.siwane.shizucorefetch.ui.theme.ShizuCoreFetchTheme
import xyz.siwane.shizucorefetch.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    // تعريف الـ ViewModel على مستوى الكلاس ليسهل الوصول إليه من دوال الـ Intent
    private lateinit var mainViewModel: MainViewModel

    // === الدالة المضافة لإجبار النظام على اتباع لغة التطبيق من الجذور ===
    // ملاحظة: كانت قبل كتقرا من "ShizuCoreFetchPrefs"/"language_code" — مفتاح
    // ماكاينش أي مكان تاني فالتطبيق كيكتب فيه، فكانت ديمًا كترجع للافتراضي "en"
    // بلا ما تعكس اللغة الحقيقية المختارة (اللي متخزنة فـ "ShizuSettings"/"language"
    // ومستعملة فعليًا من طرف MainViewModel وCompose). دابا كتقرا من نفس المصدر
    // الحقيقي، ونفس منطق "لغة النظام إلا كانت مدعومة، وإلا الإنجليزية افتراضيًا"
    // المستعمل فأول تشغيل.
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("ShizuSettings", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language", null) ?: run {
            val systemLang = Locale.getDefault().language
            val supportedCodes = setOf("ar", "en", "fr", "es", "pt", "ru", "hi", "ja", "zh", "tr", "cs")
            if (systemLang in supportedCodes) systemLang else "en"
        }

        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val localizedContext = newBase.createConfigurationContext(config)

        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // تهيئة قنوات الإشعارات
        NotificationHelper.initChannels(this)

        // تفعيل كاش GitHub HTTP
        xyz.siwane.shizucorefetch.network.NetworkModule.init(applicationContext)

        // تسجيل مستمعات Shizuku
        ShizukuManager.register()

        mainViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

        // معالجة الاختصارات عند فتح التطبيق لأول مرة (من الصفر)
        handleIntent(intent)

        setContent {
            val context = LocalContext.current

            // إدارة حالة ظهور رسالة الشفافية باستخدام SharedPreferences
            val sharedPrefs = context.getSharedPreferences("ShizuCoreFetchPrefs", Context.MODE_PRIVATE)
            var showExplanationDialog by remember { 
                mutableStateOf(sharedPrefs.getBoolean("show_github_explanation", true)) 
            }
            
            // تتبع ما إذا تم الانتهاء من طلب صلاحيات النظام
            var permissionsHandled by remember { mutableStateOf(false) }

            // مُطلق صلاحيات متعددة (Multiple Permissions)
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { permissions ->
                    // بمجرد أن ينتهي المستخدم من الصلاحيات (سواء بالرفض أو القبول)، نعتبرها اكتملت
                    permissionsHandled = true
                }
            )

            // طلب الصلاحيات الضرورية بناءً على إصدار النظام
            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf<String>()

                // 1. طلب صلاحية الإشعارات (أندرويد 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // 2. طلب صلاحية التخزين (أندرويد 10 وما دون)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }

                if (permissionsToRequest.isNotEmpty()) {
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                } else {
                    // إذا لم تكن هناك صلاحيات مطلوبة، نعتبر الأمر مكتمل فوراً
                    permissionsHandled = true
                }
            }

            val themeMode by mainViewModel.themeMode.collectAsState()
            val colorIndex by mainViewModel.themeColorIndex.collectAsState()
            val isDynamicColor by mainViewModel.isDynamicColor.collectAsState()
            val useCustomColor by mainViewModel.useCustomColor.collectAsState()
            val customHexColor by mainViewModel.customHexColor.collectAsState()
            val languageCode by mainViewModel.languageCode.collectAsState()
            val uiScale by mainViewModel.uiScale.collectAsState()

            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            val localizedContext = context.createConfigurationContext(config)

            val layoutDirection = if (languageCode == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
            
            val currentDensity = LocalDensity.current
            val customDensity = Density(
                density = currentDensity.density * uiScale,
                fontScale = currentDensity.fontScale * uiScale
            )

            ShizuCoreFetchTheme(
                themeMode = themeMode,
                colorIndex = colorIndex,
                isDynamicColor = isDynamicColor,
                useCustomColor = useCustomColor,
                customHexColor = customHexColor
            ) {
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalConfiguration provides config,
                    LocalLayoutDirection provides layoutDirection,
                    LocalDensity provides customDensity
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // الدخول المباشر للشاشة الرئيسية
                        MainScreen(mainViewModel = mainViewModel)
                        
                        // عرض رسالة الشفافية فقط إذا لم يوافق عليها سابقاً، وفقط بعد انتهاء نوافذ الصلاحيات
                        if (showExplanationDialog && permissionsHandled) {
                            AlertDialog(
                                onDismissRequest = { /* يمنع إغلاق النافذة بالنقر خارجها لإجباره على القراءة */ },
                                title = { 
                                    Text(
                                        // استخدام localizedContext بدلاً من stringResource العادية لفرض اللغة
                                        text = localizedContext.getString(R.string.github_explanation_title), 
                                        fontWeight = FontWeight.Bold 
                                    ) 
                                },
                                text = { 
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                        Text(text = localizedContext.getString(R.string.github_explanation_body))
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            // حفظ الموافقة لكي لا تظهر الرسالة مجدداً
                                            sharedPrefs.edit().putBoolean("show_github_explanation", false).apply()
                                            showExplanationDialog = false
                                        }
                                    ) {
                                        Text(text = localizedContext.getString(R.string.github_explanation_agree))
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/Si1xyz"))
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Text(text = localizedContext.getString(R.string.github_explanation_contact))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // معالجة الاختصارات إذا كان التطبيق يعمل مسبقاً في الخلفية
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    // فحص إجباري وكامل للتطبيقات المثبتة كل مرة يرجع فيها المستخدم للتطبيق —
    // كيغطي حالة تثبيت/حذف تطبيق من برا (إعدادات النظام، متجر آخر، Shizuku
    // مباشرة...) بينما التطبيق كان فالخلفية، حتى تبقى حالة الأزرار (تثبيت/
    // تحديث/فتح) دقيقة ديمًا فور الرجوع، بلا حاجة لسحب يدوي للتحديث.
    override fun onResume() {
        super.onResume()
        ShizukuManager.refreshState()
        mainViewModel.refreshInstalledApps()
    }

    // دالة التقاط الـ Intent وإرساله للـ ViewModel
    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        if (action == "ACTION_OPEN_LIBRARY" || 
            action == "ACTION_OPEN_APK_MANAGER" || 
            action == "ACTION_OPEN_SETTINGS") {
            mainViewModel.setShortcutAction(action)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuManager.unregister()
    }
}
