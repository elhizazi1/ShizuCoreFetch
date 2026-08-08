package xyz.siwane.shizucorefetch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.siwane.shizucorefetch.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupThemeDialog()
        setupLanguageDialog()
        setupSilentInstallToggle()
        setupPermissions()
        setupLinks()
    }

    private fun setupSilentInstallToggle() {
        binding.btnSilentInstall.setOnClickListener {
            val options = arrayOf(
                getString(R.string.silent_install_enabled),
                getString(R.string.silent_install_disabled)
            )
            val currentIndex = if (SettingsManager.isSilentInstallEnabled(requireContext())) 0 else 1

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.settings_silent_install_title))
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    val enable = (which == 0)
                    SettingsManager.setSilentInstallEnabled(requireContext(), enable)
                    dialog.dismiss()
                    Toast.makeText(requireContext(), options[which], Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun setupThemeDialog() {
        binding.btnTheme.setOnClickListener {
            val themes = arrayOf(
                getString(R.string.theme_light),
                getString(R.string.theme_dark),
                getString(R.string.theme_system)
            )
            
            val currentThemeIndex = when (ThemeManager.getSavedTheme(requireContext())) {
                ThemeManager.THEME_LIGHT -> 0
                ThemeManager.THEME_DARK -> 1
                else -> 2
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_theme_title))
                .setSingleChoiceItems(themes, currentThemeIndex) { dialog, which ->
                    when (which) {
                        0 -> ThemeManager.saveTheme(requireContext(), ThemeManager.THEME_LIGHT)
                        1 -> ThemeManager.saveTheme(requireContext(), ThemeManager.THEME_DARK)
                        2 -> ThemeManager.saveTheme(requireContext(), ThemeManager.THEME_SYSTEM)
                    }
                    dialog.dismiss()
                    requireActivity().recreate() 
                }
                .show()
        }
    }

    private fun setupLanguageDialog() {
        binding.btnLanguage.setOnClickListener {
            val languages = arrayOf("English", "العربية", "Français", "Español", "Português", "Русский", "हिन्दी", "中文", "日本語", "Čeština", "Türkçe")
            val langCodes = arrayOf(
                LanguageManager.LANG_EN, 
                LanguageManager.LANG_AR, 
                LanguageManager.LANG_FR, 
                LanguageManager.LANG_ES, 
                LanguageManager.LANG_PT, 
                LanguageManager.LANG_RU, 
                LanguageManager.LANG_HI, 
                LanguageManager.LANG_ZH,
                LanguageManager.LANG_JA,
                LanguageManager.LANG_CS, 
                LanguageManager.LANG_TR  
            )

            val currentLangCode = LanguageManager.getSavedLanguage(requireContext())
            val currentIndex = langCodes.indexOf(currentLangCode).takeIf { it >= 0 } ?: 0

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_lang_title))
                .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                    LanguageManager.saveLanguage(requireContext(), langCodes[which])
                    dialog.dismiss()
                    requireActivity().recreate()
                }
                .show()
        }
    }

    private fun setupPermissions() {
        binding.btnPermissionApps.setOnClickListener {
            try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${requireContext().packageName}")
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
            }
        }

        binding.btnPermissionShizuku.setOnClickListener {
            try {
                if (rikka.shizuku.Shizuku.pingBinder()) {
                    if (rikka.shizuku.Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        rikka.shizuku.Shizuku.requestPermission(100)
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.toast_shizuku_granted), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.toast_shizuku_not_running), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.toast_shizuku_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLinks() {
        binding.btnAbout.setOnClickListener {
            openWebPage("https://shizucorefetch.siwane.xyz")
        }
        binding.btnContributors.setOnClickListener {
    // استخدمنا مسار /contributors كمثال، يمكنك تغييره حسب اسم الصفحة التي ستنشئها
    openWebPage("https://shizucorefetch.siwane.xyz/contributors") 
}

        binding.btnPrivacy.setOnClickListener {
            openWebPage("https://shizucorefetch.siwane.xyz/privacy")
        }

        binding.btnTerms.setOnClickListener {
            openWebPage("https://shizucorefetch.siwane.xyz/terms")
        }
    }

    private fun openWebPage(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "لا يوجد متصفح مثبت", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
