package xyz.siwane.shizucorefetch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import xyz.siwane.shizucorefetch.databinding.FragmentDeveloperProfileBinding
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class DeveloperProfileFragment : Fragment() {

    private var _binding: FragmentDeveloperProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var developerUsername: String
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val ARG_DEVELOPER_USERNAME = "DEVELOPER_USERNAME"

        fun newInstance(developerUsername: String): DeveloperProfileFragment {
            val fragment = DeveloperProfileFragment()
            val args = Bundle()
            args.putString(ARG_DEVELOPER_USERNAME, developerUsername)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDeveloperProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        developerUsername = arguments?.getString(ARG_DEVELOPER_USERNAME) ?: return

        setupSwipeRefresh()

        val allApps = StoreCacheManager.getCachedApps(requireContext()) ?: emptyList()
        populateUI(allApps)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshDevProfile.setColorSchemeResources(
            androidx.appcompat.R.color.abc_btn_colored_borderless_text_material,
            androidx.appcompat.R.color.abc_primary_text_material_light
        )
        binding.swipeRefreshDevProfile.setOnRefreshListener {
            fetchDataFromNetwork()
        }
    }

    private fun fetchDataFromNetwork() {
        RetrofitClient.instance.getApps(Constants.GAS_URL).enqueue(object : Callback<List<AppModel>> {
            override fun onResponse(call: Call<List<AppModel>>, response: Response<List<AppModel>>) {
                if (!isAdded) return
                binding.swipeRefreshDevProfile.isRefreshing = false

                if (response.isSuccessful && response.body() != null) {
                    val newApps = response.body()!!
                    StoreCacheManager.saveApps(requireContext(), newApps)
                    populateUI(newApps)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.store_fetch_error_network), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<AppModel>>, t: Throwable) {
                if (!isAdded) return
                binding.swipeRefreshDevProfile.isRefreshing = false
                Toast.makeText(requireContext(), "${getString(R.string.store_fetch_error_network)} ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun populateUI(allApps: List<AppModel>) {
        val devApps = allApps.filter { it.developer.equals(developerUsername, ignoreCase = true) }

        if (devApps.isEmpty()) {
            binding.tvDevProfileName.text = developerUsername
            binding.tvDevProfileMessage.visibility = View.GONE
            val githubAvatarUrl = "https://github.com/$developerUsername.png"
            binding.ivDevProfileAvatar.load(githubAvatarUrl) { crossfade(true) }
            binding.ivDevProfileBanner.load(githubAvatarUrl) { crossfade(true) }
            return
        }

        val richApp = devApps.find { it.hasJsonStore } ?: devApps.first()

        val currentLang = Locale.getDefault().language
        val finalDevName = if (currentLang == "ar" && richApp.developerNameAr.isNotEmpty()) richApp.developerNameAr else developerUsername
        val finalDevMessage = if (currentLang == "ar" && richApp.developerMessageAr.isNotEmpty()) richApp.developerMessageAr else richApp.developerMessage

        val githubAvatarUrl = "https://github.com/$developerUsername.png"

        binding.tvDevProfileName.text = finalDevName

        if (finalDevMessage.isNotEmpty()) {
            binding.tvDevProfileMessage.visibility = View.VISIBLE
            binding.tvDevProfileMessage.text = finalDevMessage
        } else {
            binding.tvDevProfileMessage.visibility = View.GONE
        }

        binding.ivDevProfileAvatar.load(githubAvatarUrl) {
            crossfade(true)
        }

        if (richApp.developerBanner.isNotEmpty()) {
            binding.ivDevProfileBanner.load(richApp.developerBanner) {
                crossfade(true)
            }
        } else {
            binding.ivDevProfileBanner.load(githubAvatarUrl) {
                crossfade(true)
            }
        }

        if (richApp.hasJsonStore) {
            fetchDeveloperDetailsFromGithub(richApp.developer, richApp.name, finalDevName)
        }

        binding.rvDevProfileApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDevProfileApps.setHasFixedSize(true)

        val adapter = StoreAdapter(devApps) { appItem, packageName ->
            openAppDetails(appItem, packageName)
        }
        binding.rvDevProfileApps.adapter = adapter
    }

    private fun fetchDeveloperDetailsFromGithub(developer: String, repoName: String, finalDevName: String) {
        thread {
            try {
                val isOfficialShizuku = developer.equals("rikkaapps", ignoreCase = true) && repoName.equals("shizuku", ignoreCase = true)
                val apiUrl = if (isOfficialShizuku) "https://api.github.com/repos/elhizazi1/DrawixPRO/contents/shizuku_official_store.json" else "https://api.github.com/repos/$developer/$repoName/contents/shizu_store.json"
                val result = GithubClient.get(requireContext(), apiUrl, "application/vnd.github.v3.raw")

                if (result.code == 200 && result.body != null) {
                    var jsonText = result.body.trim()
                    try {
                        val tempObj = JSONObject(jsonText)
                        if (tempObj.has("content") && tempObj.optString("encoding") == "base64") {
                            val base64Content = tempObj.getString("content").replace("\n", "")
                            jsonText = String(Base64.decode(base64Content, Base64.DEFAULT), Charsets.UTF_8).trim()
                        }
                    } catch (e: Exception) { }

                    val cleanJson = if (jsonText.startsWith("\uFEFF")) jsonText.substring(1) else jsonText
                    val jsonObject = JSONObject(cleanJson)
                    val devObj = jsonObject.optJSONObject("developer")

                    if (devObj != null) {
                        mainHandler.post {
                            if (isAdded && _binding != null) {
                                populateDeveloperDetails(devObj)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun populateDeveloperDetails(devObj: JSONObject) {
        binding.cvDeveloperDetails.visibility = View.VISIBLE
        binding.llDevLinks.removeAllViews()

        val email = devObj.optString("email", "")
        val websiteUrl = devObj.optString("website", "")
        val portfolioUrl = devObj.optString("portfolio", "")

        val rowLinks = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
        }

        val createWeightedGridItem = { title: String, url: String ->
            TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = (4 * resources.displayMetrics.density).toInt()
                    marginStart = (4 * resources.displayMetrics.density).toInt()
                }
                text = title
                textSize = 12f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = android.view.Gravity.CENTER
                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                setPadding(0, 16, 0, 16)
                if (url.isNotEmpty()) {
                    setBackgroundResource(requireContext().theme.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground)).getResourceId(0, 0))
                    setOnClickListener { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { } }
                }
            }
        }

        if (email.isNotEmpty()) rowLinks.addView(createWeightedGridItem(getString(R.string.store_dev_email2), "mailto:$email"))
        else rowLinks.addView(createWeightedGridItem("", ""))

        if (websiteUrl.isNotEmpty()) rowLinks.addView(createWeightedGridItem(getString(R.string.store_dev_website), websiteUrl))
        else rowLinks.addView(createWeightedGridItem("", ""))

        if (portfolioUrl.isNotEmpty()) rowLinks.addView(createWeightedGridItem(getString(R.string.store_dev_portfolio), portfolioUrl))
        else rowLinks.addView(createWeightedGridItem("", ""))

        binding.llDevLinks.addView(rowLinks)

        val socials = devObj.optJSONObject("socials")
        if (socials != null) {
            val socialContainer = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (16 * resources.displayMetrics.density).toInt()
                }
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
            }
            val supportedSocials = listOf("facebook", "instagram", "x", "youtube", "github", "telegram")
            val typedValue = android.util.TypedValue()
            requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true)
            val rippleBackground = typedValue.resourceId

            for (platform in supportedSocials) {
                if (socials.has(platform)) {
                    val url = socials.optString(platform, "")
                    if (url.isNotEmpty()) {
                        val iconResId = when (platform) {
                            "facebook" -> R.drawable.ic_facebook
                            "instagram" -> R.drawable.ic_instagram
                            "x" -> R.drawable.ic_x
                            "youtube" -> R.drawable.ic_youtube
                            "github" -> R.drawable.ic_github
                            "telegram" -> R.drawable.ic_telegram
                            else -> 0
                        }
                        if (iconResId != 0) {
                            val imageView = ImageView(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams((36 * resources.displayMetrics.density).toInt(), (36 * resources.displayMetrics.density).toInt()).apply {
                                    marginStart = (8 * resources.displayMetrics.density).toInt()
                                    marginEnd = (8 * resources.displayMetrics.density).toInt()
                                }
                                setImageResource(iconResId)
                                setBackgroundResource(rippleBackground)
                                isClickable = true
                                isFocusable = true
                                setOnClickListener { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { } }
                            }
                            socialContainer.addView(imageView)
                        }
                    }
                }
            }
            if (socialContainer.childCount > 0) {
                val horizontalScroll = HorizontalScrollView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    isHorizontalScrollBarEnabled = false
                    isFillViewport = true
                }
                horizontalScroll.addView(socialContainer)
                binding.llDevLinks.addView(horizontalScroll)
            }
        }
    }

    private fun openAppDetails(appItem: AppModel, packageName: String) {
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
            adApproved = appItem.adApproved
        )

        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
