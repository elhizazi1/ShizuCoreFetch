package xyz.siwane.shizucorefetch

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import xyz.siwane.shizucorefetch.databinding.FragmentAccountBinding
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var installedAdapter: AccountAppAdapter
    private lateinit var updateAdapter: AccountAppAdapter
    
    private val installedAppsList = mutableListOf<AppModel>()
    private val updateAppsList = mutableListOf<AppModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        checkUserStateAndSetupUI()
    }

    private fun setupRecyclerViews() {
        binding.rvInstalledApps.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvUpdateApps.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        
        installedAdapter = AccountAppAdapter(
            appList = installedAppsList,
            isUpdateList = false,
            onAppClick = { app -> openAppDetails(app) },
            onActionClick = { app -> openApp(app) } 
        )
        binding.rvInstalledApps.adapter = installedAdapter

        updateAdapter = AccountAppAdapter(
            appList = updateAppsList,
            isUpdateList = true,
            onAppClick = { app -> openAppDetails(app) },
            onActionClick = { app -> openAppDetails(app) }
        )
        binding.rvUpdateApps.adapter = updateAdapter
    }

    private fun checkUserStateAndSetupUI() {
        val token = AuthManager.getToken(requireContext())
        
        binding.layoutAppsSection.visibility = View.VISIBLE
        binding.btnNavigateToWallet.visibility = View.VISIBLE
        
        if (token.isNullOrEmpty()) {
            binding.tvAccountName.text = getString(R.string.account_guest_name)
            binding.tvAccountEmail.text = getString(R.string.account_guest_email)
            binding.btnLogout.text = getString(R.string.main_login_github)
            binding.btnLogout.setOnClickListener { navigateToLogin(autoStartGithub = true) }
        } else {
            binding.btnLogout.text = getString(R.string.account_logout)
            binding.btnLogout.setOnClickListener {
                AuthManager.saveToken(requireContext(), "")
                navigateToLogin(autoStartGithub = false)
            }
            
            val savedUsername = AuthManager.getUsername(requireContext())
            if (!savedUsername.isNullOrEmpty()) {
                binding.tvAccountName.text = savedUsername
                binding.tvAccountEmail.text = "@$savedUsername"
            }
            
            fetchGitHubProfile(token)
        }
        
        binding.btnNavigateToWallet.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, WalletFragment())
                .addToBackStack(null)
                .commit()
        }
        
        fetchStoreAppsAndAnalyze()
    }

    private fun fetchStoreAppsAndAnalyze() {
        val cachedApps = StoreCacheManager.getCachedApps(requireContext())
        if (cachedApps != null && cachedApps.isNotEmpty()) {
            analyzeAppsInBackground(cachedApps)
        }

        RetrofitClient.instance.getApps(Constants.GAS_URL).enqueue(object : Callback<List<AppModel>> {
            override fun onResponse(call: Call<List<AppModel>>, response: Response<List<AppModel>>) {
                if (response.isSuccessful && response.body() != null) {
                    val newApps = response.body()!!
                    if (cachedApps == null || newApps != cachedApps) {
                        StoreCacheManager.saveApps(requireContext(), newApps)
                        analyzeAppsInBackground(newApps)
                    }
                }
            }

            override fun onFailure(call: Call<List<AppModel>>, t: Throwable) {
                if (cachedApps == null && isAdded) {
                    Toast.makeText(requireContext(), getString(R.string.store_fetch_error_network), Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun analyzeAppsInBackground(storeApps: List<AppModel>) {
        val safeContext = context ?: return
        val pm = safeContext.packageManager

        thread {
            val tempInstalled = CopyOnWriteArrayList<AppModel>()
            val tempUpdate = CopyOnWriteArrayList<AppModel>()
            val networkThreads = mutableListOf<Thread>()

            val allInstalledApps = try {
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            } catch (e: Exception) {
                emptyList()
            }

            for (app in storeApps) {
                var packageName = AppCacheManager.getSavedPackageFast(safeContext, app.id) ?: ""
                var installedAppInfo: ApplicationInfo? = null
                
                if (packageName.isEmpty()) {
                    val cleanAppName = app.name.replace(" ", "").lowercase()
                    for (appInfo in allInstalledApps) {
                        val pkgName = appInfo.packageName.lowercase()
                        val label = pm.getApplicationLabel(appInfo).toString().replace(" ", "").lowercase()
                        
                        if (pkgName.contains(cleanAppName) || label == cleanAppName || label.contains(cleanAppName) || cleanAppName.contains(label)) {
                            packageName = appInfo.packageName
                            installedAppInfo = appInfo
                            AppCacheManager.savePackageName(safeContext, app.id, packageName)
                            break
                        }
                    }
                } else {
                    installedAppInfo = allInstalledApps.find { it.packageName == packageName }
                }
                
                if (packageName.isNotEmpty() && installedAppInfo != null) {
                    val isSystemApp = (installedAppInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || 
                                      (installedAppInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                    tempInstalled.add(app)
                    if (isSystemApp) continue

                    val t = thread {
                        try {
                            val pInfo = pm.getPackageInfo(packageName, 0)
                            val installedVersion = pInfo.versionName ?: ""

                            val apiUrl = "https://api.github.com/repos/${app.developer}/${app.name}/releases/latest"
                            val result = GithubClient.get(safeContext, apiUrl, "application/vnd.github.v3+json")

                            if (result.code == 200 && result.body != null) {
                                val json = JSONObject(result.body)
                                val latestVersion = json.getString("tag_name")

                                if (VersionHelper.isVersionNewer(installedVersion, latestVersion)) {
                                    tempUpdate.add(app)
                                }
                            }
                        } catch (e: Exception) {
                        }
                    }
                    networkThreads.add(t)
                }
            }

            networkThreads.forEach { it.join() }

            mainHandler.post {
                if (isAdded && _binding != null) {
                    installedAppsList.clear()
                    installedAppsList.addAll(tempInstalled)
                    
                    updateAppsList.clear()
                    updateAppsList.addAll(tempUpdate)

                    installedAdapter.notifyDataSetChanged()
                    updateAdapter.notifyDataSetChanged()

                    binding.tvNoInstalledApps.visibility = if (installedAppsList.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvInstalledApps.visibility = if (installedAppsList.isEmpty()) View.GONE else View.VISIBLE

                    binding.tvNoUpdates.visibility = if (updateAppsList.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvUpdateApps.visibility = if (updateAppsList.isEmpty()) View.GONE else View.VISIBLE

                    if (updateAppsList.isNotEmpty()) {
                        NotificationHelper.showUpdateAvailableNotification(safeContext, updateAppsList.size)
                    }
                }
            }
        }
    }

    private fun openApp(app: AppModel) {
        val safeContext = context ?: return
        
        // استدعاء الحزمة بأمان في الخلفية (الطريقة المحدثة)
        AppCacheManager.getPackageNameAsync(safeContext, app.name, app.id) { packageName ->
            if (isAdded && packageName.isNotEmpty()) {
                val intent = safeContext.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(safeContext, getString(R.string.store_cannot_open), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openAppDetails(appItem: AppModel) {
        val safeContext = context ?: return
        
        // استدعاء الحزمة بأمان في الخلفية (الطريقة المحدثة)
        AppCacheManager.getPackageNameAsync(safeContext, appItem.name, appItem.id) { packageName ->
            if (!isAdded) return@getPackageNameAsync
            
            val fragment = AppDetailsFragment.newInstance(
                appId = appItem.id,
                appName = appItem.name,
                developer = appItem.developer,
                iconUrl = appItem.iconUrl,
                desc = appItem.description,
                packageName = packageName,
                stars = appItem.stars,
                devMsg = appItem.developerMessage,
                devMsgAr = appItem.developerMessageAr,
                devNameAr = appItem.developerNameAr,
                adApproved = appItem.adApproved,
                bannerUrl = appItem.bannerUrl,
                bannerUrlAr = appItem.bannerUrlAr,
                downloads = appItem.downloads,
                category = appItem.category,
                categoryAr = appItem.categoryAr
            )
            
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, fragment) 
                .addToBackStack(null)
                .commit()
        }
    }


    private fun navigateToLogin(autoStartGithub: Boolean = false) {
        val safeContext = context ?: return
        val intent = Intent(safeContext, LoginActivity::class.java)
        intent.putExtra("auto_start_github", autoStartGithub)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun fetchGitHubProfile(token: String) {
        thread {
            try {
                val apiUrl = "https://api.github.com/user"
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonObject = JSONObject(response)

                    val loginUsername = jsonObject.getString("login")
                    val name = if (jsonObject.has("name") && !jsonObject.isNull("name")) jsonObject.getString("name") else loginUsername
                    val email = if (jsonObject.has("email") && !jsonObject.isNull("email")) jsonObject.getString("email") else "@$loginUsername"
                    val avatarUrl = if (jsonObject.has("avatar_url")) jsonObject.getString("avatar_url") else ""

                    AuthManager.saveUsername(requireContext(), loginUsername)

                    mainHandler.post {
                        if (isAdded && _binding != null) {
                            binding.tvAccountName.text = name
                            binding.tvAccountEmail.text = email
                            if (avatarUrl.isNotEmpty()) {
                                binding.ivAccountAvatar.load(avatarUrl) { crossfade(true) }
                            }
                        }
                    }
                } else {
                    mainHandler.post { loadCachedProfileFallback() }
                }
            } catch (e: Exception) {
                mainHandler.post { loadCachedProfileFallback() }
            }
        }
    }

    private fun loadCachedProfileFallback() {
        if (isAdded && _binding != null) {
            val savedUsername = AuthManager.getUsername(requireContext())
            if (!savedUsername.isNullOrEmpty()) {
                binding.tvAccountName.text = savedUsername
                binding.tvAccountEmail.text = "@$savedUsername"
            } else {
                binding.tvAccountName.text = "مستخدم GitHub"
                binding.tvAccountEmail.text = "تعذر التحديث"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
