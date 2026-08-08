package xyz.siwane.shizucorefetch

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.noties.markwon.Markwon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import rikka.shizuku.Shizuku
import xyz.siwane.shizucorefetch.databinding.FragmentAppDetailsBinding
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import java.text.NumberFormat
import java.util.Locale

class AppDetailsFragment : Fragment() {

    private var _binding: FragmentAppDetailsBinding? = null
    private val binding get() = _binding!!
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var markwon: Markwon
    
    private var currentPackageName: String = ""
    private var currentAppName: String = ""
    private var currentAppId: String = ""
    private var currentAppDeveloper: String = ""
    private var currentStoreIssueNumber: Int = 0 
    
    private var isAdApprovedFromGas: Boolean = false
    private val GAS_URL = Constants.GAS_URL

    private lateinit var versionAdapter: VersionAdapter
    private val allReleases = mutableListOf<ReleaseModel>()
    private val visibleReleases = mutableListOf<ReleaseModel>()
    private var isShowingAllVersions = false

    private lateinit var commentAdapter: CommentAdapter
    private val allComments = mutableListOf<CommentModel>()
    private val visibleComments = mutableListOf<CommentModel>()
    private var isShowingAllComments = false
    private var emptyCommentsView: TextView? = null

    companion object {
        fun newInstance(
            appId: String, appName: String, developer: String, iconUrl: String, 
            desc: String, packageName: String = "", stars: Int = 0,
            devMsg: String = "", devMsgAr: String = "", devNameAr: String = "",
            adApproved: Boolean = false, bannerUrl: String = "", bannerUrlAr: String = "",
            downloads: Long = 0, category: String = "", categoryAr: String = ""
        ): AppDetailsFragment {
            val fragment = AppDetailsFragment()
            val args = Bundle()
            args.putString("APP_ID", appId)
            args.putString("APP_NAME", appName)
            args.putString("APP_DEVELOPER", developer)
            args.putString("APP_ICON_URL", iconUrl)
            args.putString("APP_DESC", desc)
            args.putString("APP_PACKAGE", packageName)
            args.putInt("APP_STARS", stars)
            args.putString("APP_DEV_MSG", devMsg)
            args.putString("APP_DEV_MSG_AR", devMsgAr)
            args.putString("APP_DEV_NAME_AR", devNameAr)
            args.putBoolean("AD_APPROVED", adApproved) 
            args.putString("APP_BANNER_URL", bannerUrl)
            args.putString("APP_BANNER_URL_AR", bannerUrlAr)
            args.putLong("APP_DOWNLOADS", downloads)
            args.putString("APP_CATEGORY", category)
            args.putString("APP_CATEGORY_AR", categoryAr)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        markwon = Markwon.create(requireContext())
        binding.toolbarDetails.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        currentAppId = arguments?.getString("APP_ID") ?: ""
        currentAppName = arguments?.getString("APP_NAME") ?: ""
        currentAppDeveloper = arguments?.getString("APP_DEVELOPER") ?: ""
        val appIconUrl = arguments?.getString("APP_ICON_URL") ?: ""
        val appDescription = arguments?.getString("APP_DESC") ?: ""
        val appStars = arguments?.getInt("APP_STARS") ?: 0
        val appDevMsg = arguments?.getString("APP_DEV_MSG") ?: ""
        val appDevMsgAr = arguments?.getString("APP_DEV_MSG_AR") ?: ""
        val appDevNameAr = arguments?.getString("APP_DEV_NAME_AR") ?: ""
        val appBannerUrl = arguments?.getString("APP_BANNER_URL") ?: ""
        val appBannerUrlAr = arguments?.getString("APP_BANNER_URL_AR") ?: ""
        
        val appDownloads = arguments?.getLong("APP_DOWNLOADS") ?: 0L
        val appCategory = arguments?.getString("APP_CATEGORY") ?: ""
        val appCategoryAr = arguments?.getString("APP_CATEGORY_AR") ?: ""
        
        isAdApprovedFromGas = arguments?.getBoolean("AD_APPROVED") ?: false
        
        // تم إزالة الاستدعاء المتزامن الذي كان يجمد الشاشة هنا
        currentPackageName = arguments?.getString("APP_PACKAGE") ?: ""

        val currentLang = Locale.getDefault().language
        val finalDevMsg = if (currentLang == "ar" && appDevMsgAr.isNotEmpty()) appDevMsgAr else appDevMsg
        val finalDevName = if (currentLang == "ar" && appDevNameAr.isNotEmpty()) appDevNameAr else currentAppDeveloper
        val finalCategory = if (currentLang == "ar" && appCategoryAr.isNotEmpty()) appCategoryAr else appCategory

        binding.tvDetailsName.text = currentAppName
        val devPrefix = getString(R.string.store_developer_prefix)
        
        if (finalCategory.isNotEmpty()) {
            val catPrefix = getString(R.string.store_category_prefix)
            val devText = getString(R.string.store_developer_format, devPrefix, finalDevName)
            binding.tvDetailsDeveloper.text = "$devText\n$catPrefix $finalCategory"
        } else {
            binding.tvDetailsDeveloper.text = getString(R.string.store_developer_format, devPrefix, finalDevName)
        }

        binding.tvDetailsDeveloper.setOnClickListener { openDevProfile() }
        binding.cvDeveloperDetails.setOnClickListener { openDevProfile() }

        val formattedDownloads = when {
            appDownloads >= 1_000_000 -> getString(R.string.store_downloads_million, String.format(Locale.US, "%.1f", appDownloads / 1_000_000.0))
            appDownloads >= 1_000 -> getString(R.string.store_downloads_thousand, String.format(Locale.US, "%.1f", appDownloads / 1_000.0))
            else -> appDownloads.toString()
        }
        
        val tvAppDownloads = view.findViewById<TextView>(R.id.tvAppDownloads)
        val cvDownloadsBadge = view.findViewById<View>(R.id.cvDownloadsBadge)
        
        if (appDownloads > 0) {
            cvDownloadsBadge?.visibility = View.VISIBLE
            tvAppDownloads?.text = getString(R.string.store_downloads_count, formattedDownloads)
        } else {
            cvDownloadsBadge?.visibility = View.GONE
        }

        if (appDescription.isNotEmpty()) {
            markwon.setMarkdown(binding.tvDetailsReadme, appDescription)
        } else {
            binding.tvDetailsReadme.text = getString(R.string.downloader_searching)
        }
        
        setupDeveloperMessage(finalDevMsg)
        
        loadRealRatingAndGithubStars(currentAppId, appStars)
        
        binding.ivDetailsIcon.load(appIconUrl) { crossfade(true) }

        val isRtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val targetBannerUrl = if (isRtl && appBannerUrlAr.isNotEmpty()) appBannerUrlAr else appBannerUrl
        if (targetBannerUrl.isNotEmpty()) {
            binding.ivDetailsBanner.visibility = View.VISIBLE
            binding.ivDetailsBanner.load(targetBannerUrl) { crossfade(true) }
        }
        
        setupVersionsRecyclerView(currentAppDeveloper, currentAppId, currentAppName)
        
        binding.btnToggleVersions.setOnClickListener {
            isShowingAllVersions = !isShowingAllVersions
            updateVersionsVisibility()
        }
        
        setupCommentsRecyclerView() 
        setupSwipeRefresh(currentAppDeveloper, currentAppName, appDescription)
        setupTrendingApps(currentAppId)
        
        fetchStoreDataFromGithub(currentAppDeveloper, currentAppName, appDescription)
        fetchReleasesFromGithub(currentAppDeveloper, currentAppName)

        // استدعاء الحزمة بأمان في الخلفية ثم إعداد الأزرار
        if (currentPackageName.isEmpty()) {
            AppCacheManager.getPackageNameAsync(requireContext(), currentAppName, currentAppId) { pkg ->
                if (isAdded) {
                    currentPackageName = pkg
                    setupButtons(currentAppDeveloper, currentAppId, currentAppName)
                }
            }
        } else {
            setupButtons(currentAppDeveloper, currentAppId, currentAppName)
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentAppName.isNotEmpty()) {
            // تحديث حالة الأزرار بأمان في الخلفية عند العودة للواجهة
            AppCacheManager.getPackageNameAsync(requireContext(), currentAppName, currentAppId) { pkg ->
                if (isAdded) {
                    currentPackageName = pkg
                    setupButtons(currentAppDeveloper, currentAppId, currentAppName)
                }
            }
        }
    }

    private fun loadRealRatingAndGithubStars(appId: String, githubStars: Int) {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        val formattedStars = formatter.format(githubStars)
        
        CoroutineScope(Dispatchers.Main).launch {
            val stats = RatingManager.getAppRatingStats(appId)
            
            if (stats.totalRatings > 0) {
                binding.tvRatingScore.text = String.format(Locale.US, "%.1f", stats.averageRating)
                val starsText = getString(R.string.store_github_stars_format, formattedStars)
                val ratingsText = getString(R.string.store_real_ratings_count, stats.totalRatings)
                binding.tvRatingCount.text = getString(R.string.store_rating_combined, starsText, ratingsText)
            } else {
                binding.tvRatingScore.text = getString(R.string.store_default_rating)
                val starsText = getString(R.string.store_github_stars_format, formattedStars)
                val noRatingsText = getString(R.string.store_no_real_ratings_yet)
                binding.tvRatingCount.text = getString(R.string.store_rating_combined, starsText, noRatingsText)
            }
            
            binding.rbAppRating.setOnRatingBarChangeListener(null)
            
            val userRating = RatingManager.getCurrentUserRating(requireContext(), appId)
            if (userRating != null) {
                binding.rbAppRating.rating = userRating
            } else {
                binding.rbAppRating.rating = stats.averageRating
            }
            
            binding.rbAppRating.setOnRatingBarChangeListener { _, rating, fromUser ->
                if (fromUser) {
                    submitUserRating(appId, rating, githubStars)
                }
            }
        }
    }

    private fun submitUserRating(appId: String, rating: Float, githubStars: Int) {
        if (!AuthManager.hasValidToken(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.login_prompt_rating), Toast.LENGTH_SHORT).show()
            loadRealRatingAndGithubStars(appId, githubStars)
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val success = RatingManager.submitRating(requireContext(), appId, rating)
            if (success) {
                if (isAdded) {
                    Toast.makeText(requireContext(), getString(R.string.store_rating_success), Toast.LENGTH_SHORT).show()
                }
                loadRealRatingAndGithubStars(appId, githubStars)
            } else {
                if (isAdded) {
                    Toast.makeText(requireContext(), getString(R.string.store_rating_fail), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openDevProfile() {
        if (currentAppDeveloper.isNotEmpty()) {
            val devProfileFragment = DeveloperProfileFragment.newInstance(currentAppDeveloper)
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, devProfileFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupTrendingApps(currentAppId: String) {
        val rvTrendingApps = view?.findViewById<RecyclerView>(R.id.rvTrendingApps)
        val tvTrendingAppsTitle = view?.findViewById<TextView>(R.id.tvTrendingAppsTitle)

        val allApps = StoreCacheManager.getCachedApps(requireContext()) ?: emptyList()
        val trendingApps = allApps.filter { it.id != currentAppId }.sortedByDescending { it.stars }.take(8) 

        if (trendingApps.isEmpty()) {
            rvTrendingApps?.visibility = View.GONE
            tvTrendingAppsTitle?.visibility = View.GONE
            return
        }

        rvTrendingApps?.visibility = View.VISIBLE
        tvTrendingAppsTitle?.visibility = View.VISIBLE

        rvTrendingApps?.layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.HORIZONTAL, false)
        rvTrendingApps?.isNestedScrollingEnabled = false 

        val adapter = TrendingAppAdapter(trendingApps) { clickedApp ->
            val fragment = newInstance(
                appId = clickedApp.id,
                appName = clickedApp.name,
                developer = clickedApp.developer,
                iconUrl = clickedApp.iconUrl,
                desc = clickedApp.description,
                packageName = "", 
                stars = clickedApp.stars,
                devMsg = clickedApp.developerMessage,
                devMsgAr = clickedApp.developerMessageAr,
                devNameAr = clickedApp.developerNameAr,
                adApproved = clickedApp.adApproved,
                bannerUrl = clickedApp.bannerUrl,
                bannerUrlAr = clickedApp.bannerUrlAr,
                downloads = clickedApp.downloads,
                category = clickedApp.category,
                categoryAr = clickedApp.categoryAr
            )

            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null) 
                .commit()
        }

        rvTrendingApps?.adapter = adapter
    }

    private fun setupCommentsRecyclerView() {
        val rvComments = view?.findViewById<RecyclerView>(R.id.rvComments)
        rvComments?.layoutManager = LinearLayoutManager(requireContext())
        rvComments?.isNestedScrollingEnabled = false

        commentAdapter = CommentAdapter(
            commentsList = visibleComments,
            markwon = markwon,
            onReplyClick = { username -> handleReplyClick(username) },
            onEditClick = { commentId, body -> handleEditClick(commentId, body) },
            onDeleteClick = { commentId -> handleDeleteClick(commentId) },
            onReactClick = { commentId -> showReactionDialog(commentId) }
        )
        rvComments?.adapter = commentAdapter

        val btnToggleComments = view?.findViewById<TextView>(R.id.btnToggleComments)
        btnToggleComments?.setOnClickListener {
            isShowingAllComments = !isShowingAllComments
            updateCommentsVisibility()
        }
        
        val container = rvComments?.parent as? ViewGroup
        if (container != null && emptyCommentsView == null) {
            emptyCommentsView = TextView(requireContext()).apply {
                text = getString(R.string.empty_comments_message)
                setPadding(16, 16, 16, 16)
                visibility = View.GONE
            }
            val index = container.indexOfChild(rvComments)
            container.addView(emptyCommentsView, index)
        }
    }

    private fun handleReplyClick(username: String) {
        val etComment = view?.findViewById<EditText>(R.id.etCommentInput)
        etComment?.let {
            val replyText = "> @$username\n\n"
            it.setText(replyText)
            it.setSelection(it.text.length)
            it.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun handleEditClick(commentId: Long, currentBody: String) {
        val input = EditText(requireContext()).apply {
            setText(currentBody)
            setPadding(32, 32, 32, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.comment_edit_button))
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newBody = input.text.toString()
                if (newBody.isNotEmpty() && newBody != currentBody) {
                    editCommentOnGithub(commentId, newBody)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun handleDeleteClick(commentId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_comment_title)) 
            .setMessage(getString(R.string.delete_comment_message))
            .setPositiveButton(android.R.string.ok) { _, _ -> deleteCommentFromGithub(commentId) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun editCommentOnGithub(commentId: Long, newBody: String) {
        val developer = arguments?.getString("APP_DEVELOPER") ?: ""
        val repoName = currentAppName
        thread {
            try {
                val apiUrl = "https://api.github.com/repos/$developer/$repoName/issues/comments/$commentId"
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "PATCH"
                connection.doOutput = true
                val token = AuthManager.getToken(requireContext())
                if (token.isNullOrEmpty()) return@thread

                connection.setRequestProperty("Authorization", "token $token")
                connection.setRequestProperty("Content-Type", "application/json")

                val json = JSONObject().put("body", newBody)
                connection.outputStream.write(json.toString().toByteArray())

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    mainHandler.post { 
                        Toast.makeText(requireContext(), getString(R.string.comment_edit_success), Toast.LENGTH_SHORT).show()
                        fetchCommentsFromGithub(developer, repoName, currentStoreIssueNumber) 
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun deleteCommentFromGithub(commentId: Long) {
        val developer = arguments?.getString("APP_DEVELOPER") ?: ""
        val repoName = currentAppName
        val currentUsername = AuthManager.getUsername(requireContext())
        thread {
            try {
                val apiUrl = "https://api.github.com/repos/$developer/$repoName/issues/comments/$commentId"
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                val token = AuthManager.getToken(requireContext())
                if (!token.isNullOrEmpty()) connection.setRequestProperty("Authorization", "token $token")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == HttpURLConnection.HTTP_OK) {
                    mainHandler.post { 
                        Toast.makeText(requireContext(), getString(R.string.comment_delete_success), Toast.LENGTH_SHORT).show()
                        fetchCommentsFromGithub(developer, repoName, currentStoreIssueNumber) 
                    }
                } else {
                    if (currentUsername == "elhizazi1") {
                        mainHandler.post { Toast.makeText(requireContext(), getString(R.string.comment_protected_blacklist), Toast.LENGTH_SHORT).show() }
                        blacklistCommentInGas(commentId, developer, repoName)
                    } else {
                        mainHandler.post { Toast.makeText(requireContext(), getString(R.string.comment_delete_no_permission), Toast.LENGTH_LONG).show() }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun blacklistCommentInGas(commentId: Long, developer: String, repoName: String) {
        if (GAS_URL.isEmpty()) return
        val ownerToken = AuthManager.getToken(requireContext())
        if (ownerToken.isNullOrEmpty()) return
        thread {
            try {
                val encodedToken = java.net.URLEncoder.encode(ownerToken, "UTF-8")
                val params = "action=blacklist_comment&owner_token=$encodedToken&comment_id=$commentId"
                val result = GasHttpClient.postForm(GAS_URL, params)
                val success = result.code == HttpURLConnection.HTTP_OK && result.body != null &&
                    JSONObject(result.body).optBoolean("success", false)

                mainHandler.post {
                    if (!isAdded) return@post
                    if (success) {
                        Toast.makeText(requireContext(), getString(R.string.comment_blacklist_success), Toast.LENGTH_SHORT).show()
                        fetchCommentsFromGithub(developer, repoName, currentStoreIssueNumber)
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.comment_blacklist_fail), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch(e: Exception) { e.printStackTrace() }
        }
    }

    private fun showReactionDialog(commentId: Long) {
        if (!AuthManager.hasValidToken(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.login_prompt_comments), Toast.LENGTH_SHORT).show()
            return
        }

        val reactionEmojis = arrayOf("\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDE04", "\uD83C\uDF89", "\uD83D\uDE15", "\u2764\uFE0F", "\uD83D\uDE80", "\uD83D\uDC40")
        val reactionKeys = arrayOf("+1", "-1", "laugh", "hooray", "confused", "heart", "rocket", "eyes")

        val scrollView = HorizontalScrollView(requireContext()).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER 
            isFillViewport = true 
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 32, 16, 32) 
            gravity = android.view.Gravity.CENTER 
        }

        val dialog = AlertDialog.Builder(requireContext()).setView(scrollView).create()
        val density = resources.displayMetrics.density
        val sizePx = (38 * density).toInt() 
        val marginPx = (4 * density).toInt()

        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true)
        val rippleBackground = typedValue.resourceId

        for (i in reactionEmojis.indices) {
            val tvEmoji = TextView(requireContext()).apply {
                text = reactionEmojis[i]
                textSize = 24f 
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    marginEnd = if (i == reactionEmojis.lastIndex) 0 else marginPx
                }
                setBackgroundResource(rippleBackground)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    postReactionToGithub(commentId, reactionKeys[i])
                    dialog.dismiss() 
                }
            }
            layout.addView(tvEmoji)
        }

        scrollView.addView(layout)
        dialog.show()

        val customBackground = android.graphics.drawable.GradientDrawable().apply {
            setColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorSurface))
            cornerRadius = 8f * density 
        }
        val insetMargin = (32 * density).toInt() 
        val insetDrawable = android.graphics.drawable.InsetDrawable(customBackground, insetMargin)
        dialog.window?.setBackgroundDrawable(insetDrawable)
    }

    private fun postReactionToGithub(commentId: Long, reactionContent: String) {
        val developer = arguments?.getString("APP_DEVELOPER") ?: ""
        val repoName = currentAppName
        thread {
            try {
                val apiUrl = "https://api.github.com/repos/$developer/$repoName/issues/comments/$commentId/reactions"
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Accept", "application/vnd.github.squirrel-girl-preview+json")
                val token = AuthManager.getToken(requireContext())
                if (token.isNullOrEmpty()) return@thread

                connection.setRequestProperty("Authorization", "token $token")
                connection.setRequestProperty("Content-Type", "application/json")

                val json = JSONObject().put("content", reactionContent)
                connection.outputStream.write(json.toString().toByteArray())

                if (connection.responseCode == HttpURLConnection.HTTP_CREATED || connection.responseCode == HttpURLConnection.HTTP_OK) {
                    mainHandler.post { 
                        if (isAdded) {
                            Toast.makeText(requireContext(), getString(R.string.reaction_added_success), Toast.LENGTH_SHORT).show()
                            if (currentStoreIssueNumber > 0) fetchCommentsFromGithub(developer, repoName, currentStoreIssueNumber)
                        }
                    }
                } else {
                    mainHandler.post { if (isAdded) Toast.makeText(requireContext(), getString(R.string.reaction_add_failed), Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setupDeveloperMessage(message: String) {
        if (message.isNotEmpty()) {
            binding.cvDeveloperMessage.visibility = View.VISIBLE
            binding.tvDeveloperMessageText.text = message
        } else {
            binding.cvDeveloperMessage.visibility = View.GONE
        }
    }

    private fun setupSwipeRefresh(appDeveloper: String, appName: String, appDescription: String) {
        binding.swipeRefreshDetails.setColorSchemeColors(android.graphics.Color.parseColor("#00d1b2"))
        binding.swipeRefreshDetails.setOnRefreshListener {
            binding.tvDetailsReadme.text = getString(R.string.downloader_searching)
            fetchStoreDataFromGithub(appDeveloper, appName, appDescription)
            fetchReleasesFromGithub(appDeveloper, appName)
        }
    }

    private fun fetchStoreDataFromGithub(developer: String, repoName: String, fallbackDesc: String) {
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
                    applyStoreData(jsonObject, developer, repoName, fallbackDesc)
                } else {
                    FastlaneMetadataFetcher.fetch(
                        requireContext(), developer, repoName,
                        onFound = { data -> applyFastlaneData(data, developer, repoName, fallbackDesc) },
                        onNotFound = { fetchReadmeFromGithub(developer, repoName, fallbackDesc) }
                    )
                }
            } catch (e: Exception) {
                fetchReadmeFromGithub(developer, repoName, fallbackDesc)
            }
        }
    }

    private fun applyFastlaneData(data: FastlaneMetadataFetcher.FastlaneData, developer: String, repoName: String, fallbackDesc: String) {
        val description = when {
            data.fullDescription.isNotEmpty() -> data.fullDescription
            data.shortDescription.isNotEmpty() -> data.shortDescription
            else -> ""
        }

        mainHandler.post {
            if (!isAdded || _binding == null) return@post
            binding.swipeRefreshDetails.isRefreshing = false

            if (description.isNotEmpty()) {
                markwon.setMarkdown(binding.tvDetailsReadme, description)
            } else {
                fetchReadmeFromGithub(developer, repoName, fallbackDesc)
            }

            if (data.featureGraphicDownloadUrl.isNotEmpty()) {
                binding.ivDetailsBanner.visibility = View.VISIBLE
                binding.ivDetailsBanner.load(buildImageUrl(developer, repoName, data.featureGraphicDownloadUrl)) { crossfade(true) }
            }
            if (data.iconDownloadUrl.isNotEmpty()) {
                binding.ivDetailsIcon.load(buildImageUrl(developer, repoName, data.iconDownloadUrl)) { crossfade(true) }
            }

            if (data.screenshotUrls.isNotEmpty()) {
                binding.tvScreenshotsTitle.visibility = View.VISIBLE
                binding.hsvScreenshots.visibility = View.VISIBLE
                binding.llScreenshots.removeAllViews()

                val density = resources.displayMetrics.density
                val heightPx = (280 * density).toInt()
                val marginPx = (8 * density).toInt()

                for (screenshot in data.screenshotUrls) {
                    if (screenshot.isEmpty()) continue
                    val imageView = ImageView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, heightPx).apply { marginEnd = marginPx }
                        adjustViewBounds = true
                    }
                    binding.llScreenshots.addView(imageView)
                    imageView.load(buildImageUrl(developer, repoName, screenshot)) {
                        crossfade(true)
                        transformations(RoundedCornersTransformation(8f * density))
                    }
                }
            }
        }
    }

    private fun applyStoreData(jsonObject: JSONObject, developer: String, repoName: String, fallbackDesc: String) {
        val currentLang = Locale.getDefault().language
        var description = jsonObject.optString("detailed_description", "")
        var bannerUrl = jsonObject.optString("banner_url", "")
        var iconUrl = jsonObject.optString("icon_url", "")
        var devMessage = jsonObject.optString("developer_message", "") 
        var appWebsiteUrl = jsonObject.optString("app_website", "")
        
        var category = jsonObject.optString("category", "")
        
        currentStoreIssueNumber = jsonObject.optInt("store_issue_number", 0)
        
        val hasAdsFlag = jsonObject.optBoolean("ad", false) || jsonObject.optBoolean("has_ads", false)
        val isAdApproved = isAdApprovedFromGas 
        val appAds = jsonObject.optJSONArray("ads")
        
        val currentUsername = AuthManager.getUsername(requireContext())
        val isStoreAdmin = (currentUsername == "elhizazi1")
        
        val devObj = jsonObject.optJSONObject("developer")
        var finalDevName = devObj?.optString("name", developer) ?: developer
        
        if (jsonObject.has("locales")) {
            val locales = jsonObject.optJSONObject("locales")
            if (locales != null && locales.has(currentLang)) {
                val localizedData = locales.optJSONObject(currentLang)
                if (localizedData != null) {
                    if (localizedData.has("detailed_description")) description = localizedData.optString("detailed_description", description)
                    if (localizedData.has("banner_url")) bannerUrl = localizedData.optString("banner_url", bannerUrl)
                    if (localizedData.has("icon_url")) iconUrl = localizedData.optString("icon_url", iconUrl)
                    if (localizedData.has("developer_message")) devMessage = localizedData.optString("developer_message", devMessage)
                    if (localizedData.has("developer_name")) finalDevName = localizedData.optString("developer_name", finalDevName)
                    if (localizedData.has("app_website")) appWebsiteUrl = localizedData.optString("app_website", appWebsiteUrl)
                    
                    if (localizedData.has("category")) category = localizedData.optString("category", category)
                }
            }
        }
        
        val screenshots = mutableListOf<String>()
        if (jsonObject.has("screenshots")) {
            val arr = jsonObject.optJSONArray("screenshots")
            if (arr != null) {
                for (i in 0 until arr.length()) screenshots.add(arr.optString(i, ""))
            }
        }
        
        mainHandler.post {
            if (!isAdded || _binding == null) return@post
            binding.swipeRefreshDetails.isRefreshing = false
            
            val devPrefix = getString(R.string.store_developer_prefix)
            if (category.isNotEmpty()) {
                val catPrefix = getString(R.string.store_category_prefix)
                val devText = getString(R.string.store_developer_format, devPrefix, finalDevName)
                binding.tvDetailsDeveloper.text = "$devText\n$catPrefix $category"
            } else {
                binding.tvDetailsDeveloper.text = getString(R.string.store_developer_format, devPrefix, finalDevName)
            }
            
            setupDeveloperMessage(devMessage)
            
            val tvTopWebsite = view?.findViewById<TextView>(R.id.tvTopWebsite)
            if (appWebsiteUrl.isNotEmpty()) {
                tvTopWebsite?.visibility = View.VISIBLE
                tvTopWebsite?.setOnClickListener { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(appWebsiteUrl))) } catch (e: Exception) { } }
            } else {
                tvTopWebsite?.visibility = View.GONE
            }
            
            if (description.isNotEmpty()) {
                markwon.setMarkdown(binding.tvDetailsReadme, description)
            } else {
                fetchReadmeFromGithub(developer, repoName, fallbackDesc)
            }
            
            if (bannerUrl.isNotEmpty()) {
                binding.ivDetailsBanner.visibility = View.VISIBLE
                binding.ivDetailsBanner.load(buildImageUrl(developer, repoName, bannerUrl)) { crossfade(true) }
            }
            if (iconUrl.isNotEmpty()) binding.ivDetailsIcon.load(buildImageUrl(developer, repoName, iconUrl)) { crossfade(true) }
            
            if (screenshots.isNotEmpty()) {
                binding.tvScreenshotsTitle.visibility = View.VISIBLE
                binding.hsvScreenshots.visibility = View.VISIBLE
                binding.llScreenshots.removeAllViews()
                
                val density = resources.displayMetrics.density
                val heightPx = (280 * density).toInt()
                val marginPx = (8 * density).toInt()

                for (screenshot in screenshots) {
                    if (screenshot.isEmpty()) continue
                    val imageView = ImageView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, heightPx).apply { marginEnd = marginPx }
                        adjustViewBounds = true
                    }
                    binding.llScreenshots.addView(imageView)
                    imageView.load(buildImageUrl(developer, repoName, screenshot)) {
                        crossfade(true)
                        transformations(RoundedCornersTransformation(8f * density))
                    }
                }
            }

            val cvAdTop = view?.findViewById<View>(R.id.cvAdTop)
            val ivAdTopImage = view?.findViewById<ImageView>(R.id.ivAdTopImage)
            val tvAdTopLabel = view?.findViewById<TextView>(R.id.tvAdTopLabel)
            val cvAdBottom = view?.findViewById<View>(R.id.cvAdBottom)
            val ivAdBottomImage = view?.findViewById<ImageView>(R.id.ivAdBottomImage)
            val tvAdBottomLabel = view?.findViewById<TextView>(R.id.tvAdBottomLabel)

            cvAdTop?.visibility = View.GONE
            cvAdBottom?.visibility = View.GONE

            if (hasAdsFlag && appAds != null && appAds.length() > 0) {
                val shouldShowAd = isAdApproved || isStoreAdmin
                if (shouldShowAd) {
                    for (i in 0 until appAds.length()) {
                        val adObj = appAds.optJSONObject(i) ?: continue
                        val position = adObj.optString("position", "")
                        val imageUrl = adObj.optString("image_url", "")
                        val targetUrl = adObj.optString("target_url", "")

                        if (imageUrl.isNotEmpty()) {
                            if (position == "top") {
                                cvAdTop?.visibility = View.VISIBLE
                                ivAdTopImage?.load(imageUrl) { crossfade(true) }
                                cvAdTop?.setOnClickListener { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))) } catch (e: Exception) {} }
                                if (!isAdApproved && isStoreAdmin) {
                                    tvAdTopLabel?.text = getString(R.string.store_ad_pending)
                                    tvAdTopLabel?.setTextColor(Color.RED)
                                } else {
                                    tvAdTopLabel?.text = getString(R.string.ad_label)
                                    tvAdTopLabel?.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary))
                                }
                            }
                            else if (position == "bottom") {
                                cvAdBottom?.visibility = View.VISIBLE
                                ivAdBottomImage?.load(imageUrl) { crossfade(true) }
                                cvAdBottom?.setOnClickListener { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))) } catch (e: Exception) {} }
                                if (!isAdApproved && isStoreAdmin) {
                                    tvAdBottomLabel?.text = getString(R.string.store_ad_pending)
                                    tvAdBottomLabel?.setTextColor(Color.RED)
                                } else {
                                    tvAdBottomLabel?.text = getString(R.string.ad_label)
                                    tvAdBottomLabel?.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary))
                                }
                            }
                        }
                    }
                }
            }
            if (devObj != null) populateDeveloperDetails(devObj, finalDevName)
            setupCommentSystem(developer, repoName, currentStoreIssueNumber)
        }
    }

    private fun setupCommentSystem(developer: String, repoName: String, issueNumber: Int) {
        val commentsSection = view?.findViewById<View>(R.id.cvCommentsSection)
        if (issueNumber <= 0) {
            commentsSection?.visibility = View.GONE
            return
        }
        
        commentsSection?.visibility = View.VISIBLE
        
        val etComment = view?.findViewById<EditText>(R.id.etCommentInput)
        val btnPost = view?.findViewById<View>(R.id.btnPostComment)
        val tvLoginPrompt = view?.findViewById<TextView>(R.id.tvLoginPrompt)
        
        etComment?.hint = getString(R.string.comment_input_hint)
        
        val isLoggedIn = AuthManager.hasValidToken(requireContext())

        if (isLoggedIn) {
            etComment?.visibility = View.VISIBLE
            btnPost?.visibility = View.VISIBLE
            tvLoginPrompt?.visibility = View.GONE
            
            btnPost?.setOnClickListener {
                val text = etComment?.text.toString()
                if (text.isNotEmpty()) {
                    postCommentToGithub(developer, repoName, issueNumber, text)
                    etComment?.text?.clear()
                }
            }
        } else {
            etComment?.visibility = View.GONE
            btnPost?.visibility = View.GONE
            tvLoginPrompt?.visibility = View.VISIBLE
            tvLoginPrompt?.text = getString(R.string.login_prompt_comments)
        }
        
        fetchCommentsFromGithub(developer, repoName, issueNumber)
    }

    private fun fetchCommentsFromGithub(developer: String, repoName: String, issueNumber: Int) {
        thread {
            try {
                val apiUrl = "https://api.github.com/repos/$developer/$repoName/issues/$issueNumber/comments"
                val result = GithubClient.get(requireContext(), apiUrl, "application/vnd.github.squirrel-girl-preview+json")

                if (result.code == 200 && result.body != null) {
                    val jsonArray = JSONArray(result.body)
                    val currentUsername = AuthManager.getUsername(requireContext())
                    val isStoreOwner = (currentUsername == "elhizazi1")
                    val fetchedComments = mutableListOf<CommentModel>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val commentObj = jsonArray.getJSONObject(i)
                        val userObj = commentObj.getJSONObject("user")
                        val commentId = commentObj.getLong("id")
                        val username = userObj.getString("login")
                        val avatarUrl = userObj.getString("avatar_url")
                        val body = commentObj.getString("body")
                        val isMyComment = (currentUsername != null && currentUsername == username)
                        val canDelete = isMyComment || isStoreOwner
                        
                        var reactionsText = ""
                        val reactionsObj = commentObj.optJSONObject("reactions")
                        if (reactionsObj != null) {
                            val likes = reactionsObj.optInt("+1", 0)
                            val dislikes = reactionsObj.optInt("-1", 0)
                            val laughs = reactionsObj.optInt("laugh", 0)
                            val hooray = reactionsObj.optInt("hooray", 0)
                            val confused = reactionsObj.optInt("confused", 0)
                            val hearts = reactionsObj.optInt("heart", 0)
                            val rocket = reactionsObj.optInt("rocket", 0)
                            val eyes = reactionsObj.optInt("eyes", 0)
                            
                            if (likes > 0) reactionsText += "\uD83D\uDC4D $likes  "
                            if (dislikes > 0) reactionsText += "\uD83D\uDC4E $dislikes  "
                            if (laughs > 0) reactionsText += "\uD83D\uDE04 $laughs  "
                            if (hooray > 0) reactionsText += "\uD83C\uDF89 $hooray  "
                            if (confused > 0) reactionsText += "\uD83D\uDE15 $confused  "
                            if (hearts > 0) reactionsText += "\u2764\uFE0F $hearts  "
                            if (rocket > 0) reactionsText += "\uD83D\uDE80 $rocket  "
                            if (eyes > 0) reactionsText += "\uD83D\uDC40 $eyes  "
                        }
                        fetchedComments.add(CommentModel(commentId, username, avatarUrl, body, isMyComment, canDelete, reactionsText.trim()))
                    }
                    mainHandler.post {
                        if (!isAdded) return@post
                        allComments.clear()
                        allComments.addAll(fetchedComments)
                        updateCommentsVisibility()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateCommentsVisibility() {
        visibleComments.clear()
        val btnToggleComments = view?.findViewById<TextView>(R.id.btnToggleComments)
        val rvComments = view?.findViewById<RecyclerView>(R.id.rvComments)
        
        if (allComments.isEmpty()) {
            btnToggleComments?.visibility = View.GONE
            rvComments?.visibility = View.GONE
            emptyCommentsView?.visibility = View.VISIBLE
        } else if (allComments.size <= 3) {
            visibleComments.addAll(allComments)
            btnToggleComments?.visibility = View.GONE
            rvComments?.visibility = View.VISIBLE
            emptyCommentsView?.visibility = View.GONE
        } else {
            rvComments?.visibility = View.VISIBLE
            emptyCommentsView?.visibility = View.GONE
            btnToggleComments?.visibility = View.VISIBLE
            if (isShowingAllComments) {
                visibleComments.addAll(allComments)
                btnToggleComments?.text = getString(R.string.hide_comments_previous)
            } else {
                visibleComments.addAll(allComments.take(3))
                val remaining = allComments.size - 3
                btnToggleComments?.text = getString(R.string.show_remaining_comments, remaining)
            }
        }
        commentAdapter.notifyDataSetChanged()
    }

    private fun postCommentToGithub(developer: String, repoName: String, issueNumber: Int, commentBody: String) {
        thread {
            try {
                val apiUrl = "https://api.github.com/repos/$developer/$repoName/issues/$issueNumber/comments"
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                val token = AuthManager.getToken(requireContext())
                if (token.isNullOrEmpty()) return@thread

                connection.setRequestProperty("Authorization", "token $token")
                connection.setRequestProperty("Content-Type", "application/json")

                val json = JSONObject().put("body", commentBody)
                connection.outputStream.write(json.toString().toByteArray())

                if (connection.responseCode == HttpURLConnection.HTTP_CREATED) {
                    mainHandler.post { 
                        if (isAdded) {
                            Toast.makeText(requireContext(), getString(R.string.comment_posted_success), Toast.LENGTH_SHORT).show()
                            fetchCommentsFromGithub(developer, repoName, issueNumber) 
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun populateDeveloperDetails(devObj: JSONObject, finalDevName: String) {
        if (finalDevName.isEmpty()) return
        binding.cvDeveloperDetails.visibility = View.VISIBLE
        binding.tvDevName.visibility = View.GONE 
        binding.llDevLinks.removeAllViews()

        val createGridItem = { title: String, url: String, isBold: Boolean ->
            TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = (8 * resources.displayMetrics.density).toInt() }
                text = title
                textSize = 14f
                maxLines = 1 
                ellipsize = TextUtils.TruncateAt.END
                if (isBold) {
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnSurface))
                } else {
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                }
                setPadding(0, 8, 0, 8)
                if (url.isNotEmpty()) setOnClickListener { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { } }
            }
        }

        val row1 = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.HORIZONTAL
        }
        row1.addView(createGridItem(finalDevName, "", true))
        val email = devObj.optString("email", "")
        if (email.isNotEmpty()) row1.addView(createGridItem("${getString(R.string.store_dev_email)} $email", "mailto:$email", false))
        else row1.addView(createGridItem("", "", false)) 
        binding.llDevLinks.addView(row1)

        val websiteUrl = devObj.optString("website", "")
        val portfolioUrl = devObj.optString("portfolio", "")
        if (websiteUrl.isNotEmpty() || portfolioUrl.isNotEmpty()) {
            val row2 = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = (8 * resources.displayMetrics.density).toInt() }
                orientation = LinearLayout.HORIZONTAL
            }
            if (websiteUrl.isNotEmpty()) row2.addView(createGridItem(getString(R.string.store_dev_website), websiteUrl, false)) else row2.addView(createGridItem("", "", false))
            if (portfolioUrl.isNotEmpty()) row2.addView(createGridItem(getString(R.string.store_dev_portfolio), portfolioUrl, false)) else row2.addView(createGridItem("", "", false))
            binding.llDevLinks.addView(row2)
        }
        
        val socials = devObj.optJSONObject("socials")
        if (socials != null) {
            val socialContainer = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = (24 * resources.displayMetrics.density).toInt() }
                    isHorizontalScrollBarEnabled = false
                    isFillViewport = true 
                }
                horizontalScroll.addView(socialContainer)
                binding.llDevLinks.addView(horizontalScroll)
            }
        }
    }

    private fun fetchReadmeFromGithub(developer: String, repoName: String, fallbackDesc: String) {
        thread {
            try {
                val apiUrl = "https://api.github.com/repos/$developer/$repoName/readme"
                val result = GithubClient.get(requireContext(), apiUrl, "application/vnd.github.v3.raw")

                if (result.code == 200 && result.body != null) {
                    var readmeText = result.body
                    try {
                        val tempObj = JSONObject(readmeText)
                        if (tempObj.has("content") && tempObj.optString("encoding") == "base64") {
                            val base64Content = tempObj.getString("content").replace("\n", "")
                            readmeText = String(Base64.decode(base64Content, Base64.DEFAULT), Charsets.UTF_8).trim()
                        }
                    } catch (e: Exception) { }

                    mainHandler.post {
                        if (isAdded) {
                            binding.swipeRefreshDetails.isRefreshing = false
                            markwon.setMarkdown(binding.tvDetailsReadme, readmeText)
                        }
                    }
                } else {
                    mainHandler.post { 
                        if (isAdded) { 
                            binding.swipeRefreshDetails.isRefreshing = false
                            if (fallbackDesc.isNotEmpty()) markwon.setMarkdown(binding.tvDetailsReadme, fallbackDesc)
                            else binding.tvDetailsReadme.text = getString(R.string.store_no_description)
                        } 
                    }
                }
            } catch (e: Exception) { 
                mainHandler.post { 
                    if (isAdded) { 
                        binding.swipeRefreshDetails.isRefreshing = false
                        if (fallbackDesc.isNotEmpty()) markwon.setMarkdown(binding.tvDetailsReadme, fallbackDesc)
                    } 
                } 
            }
        }
    }

    private fun fetchReleasesFromGithub(developer: String, repoName: String) {
        thread {
            try {
                val apiUrl = "https://api.github.com/repos/$developer/$repoName/releases"
                val result = GithubClient.get(requireContext(), apiUrl, "application/vnd.github.v3+json")

                if (result.code == 200 && result.body != null) {
                    val jsonArray = JSONArray(result.body)
                    val newReleases = mutableListOf<ReleaseModel>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val releaseObj = jsonArray.getJSONObject(i)
                        val versionName = releaseObj.getString("tag_name")
                        val publishedAt = releaseObj.getString("published_at").substring(0, 10)
                        val assets = releaseObj.getJSONArray("assets")
                        var downloadUrl = ""
                        var fileName = ""
                        for (j in 0 until assets.length()) {
                            val asset = assets.getJSONObject(j)
                            val name = asset.getString("name")
                            if (name.endsWith(".apk")) { downloadUrl = asset.getString("browser_download_url"); fileName = name; break }
                        }
                        if (downloadUrl.isNotEmpty()) newReleases.add(ReleaseModel(versionName, publishedAt, downloadUrl, fileName))
                    }
                    
                    mainHandler.post {
                        if (!isAdded) return@post
                        binding.swipeRefreshDetails.isRefreshing = false
                        allReleases.clear()
                        allReleases.addAll(newReleases)
                        updateVersionsVisibility()

                        val tvAppSize = view?.findViewById<TextView>(R.id.tvAppSize)
                        val cvAppSizeBadge = view?.findViewById<View>(R.id.cvAppSizeBadge)
                        
                        if (newReleases.isNotEmpty()) {
                            cvAppSizeBadge?.visibility = View.VISIBLE
                            val latestReleaseAsset = jsonArray.optJSONObject(0)?.optJSONArray("assets")
                            var latestSize = 0L
                            if (latestReleaseAsset != null) {
                                for (j in 0 until latestReleaseAsset.length()) {
                                    val asset = latestReleaseAsset.getJSONObject(j)
                                    if (asset.optString("name", "").endsWith(".apk")) { latestSize = asset.optLong("size", 0L); break }
                                }
                            }
                            if (latestSize > 0) {
                                val sizeInMb = latestSize / (1024.0 * 1024.0)
                                tvAppSize?.text = String.format(Locale.US, "%.1f MB", sizeInMb)
                            } else tvAppSize?.text = getString(R.string.store_unknown_size)
                        } else cvAppSizeBadge?.visibility = View.GONE
                    }
                } else mainHandler.post { if (isAdded) binding.swipeRefreshDetails.isRefreshing = false }
            } catch (e: Exception) { mainHandler.post { if (isAdded) binding.swipeRefreshDetails.isRefreshing = false } }
        }
    }

    private fun updateVersionsVisibility() {
        visibleReleases.clear()
        if (allReleases.size <= 2) {
            visibleReleases.addAll(allReleases)
            binding.btnToggleVersions.visibility = View.GONE
        } else {
            binding.btnToggleVersions.visibility = View.VISIBLE
            if (isShowingAllVersions) {
                visibleReleases.addAll(allReleases)
                binding.btnToggleVersions.text = getString(R.string.store_hide_versions)
            } else {
                visibleReleases.addAll(allReleases.take(2)) 
                binding.btnToggleVersions.text = getString(R.string.store_show_all_versions)
            }
        }
        versionAdapter.notifyDataSetChanged()
    }

    private fun buildImageUrl(developer: String, repoName: String, path: String): String {
        if (path.startsWith("http")) return path
        return "https://cdn.jsdelivr.net/gh/$developer/$repoName/$path"
    }

    private fun setupVersionsRecyclerView(developer: String, appId: String, appName: String) {
        binding.rvVersions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVersions.isNestedScrollingEnabled = false
        versionAdapter = VersionAdapter(visibleReleases) { release -> installSpecificVersion(developer, appId, appName, release) }
        binding.rvVersions.adapter = versionAdapter
    }

    private fun setupButtons(developer: String, appId: String, appName: String) {
        if (currentPackageName.isEmpty()) {
            setUIForInstallState(developer, appId, appName)
            return
        }
        val pm = requireContext().packageManager
        val isInstalled = try { pm.getPackageInfo(currentPackageName, 0); true } catch (e: Exception) { false }

        if (isInstalled) setUIForOpenState(currentPackageName, developer, appId, appName)
        else setUIForInstallState(developer, appId, appName)
    }

    private fun setUIForInstallState(developer: String, appId: String, appName: String) {
        binding.cvDetailsIcon.radius = 8f * resources.displayMetrics.density
        binding.pbDetailsProgress.visibility = View.GONE
        binding.btnDetailsUninstall.visibility = View.GONE
        binding.btnDetailsInstall.text = getString(R.string.install_button)
        binding.btnDetailsInstall.isEnabled = true
        binding.btnDetailsInstall.setOnClickListener { installApp(developer, appId, appName) }
    }

    private fun setUIForOpenState(packageName: String, developer: String, appId: String, appName: String) {
        binding.cvDetailsIcon.radius = 8f * resources.displayMetrics.density
        binding.pbDetailsProgress.visibility = View.GONE
        val isSystemApp = try {
            val appInfo = requireContext().packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (e: Exception) { false }

        if (isSystemApp) binding.btnDetailsUninstall.visibility = View.GONE
        else {
            binding.btnDetailsUninstall.visibility = View.VISIBLE
            binding.btnDetailsUninstall.isEnabled = true
            binding.btnDetailsUninstall.setOnClickListener { uninstallApp(packageName, developer, appId, appName) }
        }

        binding.btnDetailsInstall.text = getString(R.string.open_button)
        binding.btnDetailsInstall.isEnabled = true
        binding.btnDetailsInstall.setOnClickListener {
            val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) startActivity(launchIntent)
            else Toast.makeText(requireContext(), R.string.store_cannot_open, Toast.LENGTH_SHORT).show()
        }

        val localVersion = VersionHelper.getLocalVersion(requireContext(), packageName)
        if (localVersion != null) {
            VersionHelper.checkUpdateAvailable(requireContext(), developer, appName, localVersion) { hasUpdate, _ ->
                if (isAdded && hasUpdate) {
                    binding.btnDetailsInstall.text = getString(R.string.update_button)
                    binding.btnDetailsInstall.setOnClickListener { installApp(developer, appId, appName) }
                }
            }
        }
    }

    private fun installApp(developer: String, appId: String, appName: String) {
        if (!AuthManager.hasValidToken(requireContext())) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.guest_login_prompt_title)
                .setMessage(R.string.guest_login_prompt_install_msg)
                .setPositiveButton(R.string.guest_action_login) { _, _ ->
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.putExtra("auto_start_github", true)
                    startActivity(intent)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            return
        }

        val isSilentEnabled = SettingsManager.isSilentInstallEnabled(requireContext())
        if (isSilentEnabled) {
            if (!ShizukuHelper.isShizukuRunning() || !ShizukuHelper.hasPermission()) {
                Toast.makeText(requireContext(), R.string.store_requesting_permission, Toast.LENGTH_SHORT).show()
                Shizuku.requestPermission(1001)
                return
            }
        }

        binding.btnDetailsInstall.isEnabled = false
        binding.btnDetailsInstall.text = getString(R.string.installing_button)
        binding.cvDetailsIcon.radius = 36f * resources.displayMetrics.density
        binding.pbDetailsProgress.visibility = View.VISIBLE
        
        ApkDownloader.downloadLatestRelease(
            requireContext(), developer, appName,
            onProgress = { msg -> mainHandler.post { if (isAdded) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show() } },
            onResult = { success, path -> handleInstallResult(success, path, developer, appId, appName) }
        )
    }

    private fun installSpecificVersion(developer: String, appId: String, appName: String, release: ReleaseModel) {
        if (!AuthManager.hasValidToken(requireContext())) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.guest_login_prompt_title)
                .setMessage(R.string.guest_login_prompt_install_msg)
                .setPositiveButton(R.string.guest_action_login) { _, _ ->
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.putExtra("auto_start_github", true)
                    startActivity(intent)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            return
        }

        val isSilentEnabled = SettingsManager.isSilentInstallEnabled(requireContext())
        if (isSilentEnabled) {
            if (!ShizukuHelper.isShizukuRunning() || !ShizukuHelper.hasPermission()) {
                Toast.makeText(requireContext(), R.string.store_requesting_permission, Toast.LENGTH_SHORT).show()
                Shizuku.requestPermission(1001)
                return
            }
        }

        binding.btnDetailsInstall.isEnabled = false
        binding.btnDetailsInstall.text = getString(R.string.installing_button)
        binding.cvDetailsIcon.radius = 36f * resources.displayMetrics.density
        binding.pbDetailsProgress.visibility = View.VISIBLE
        NotificationHelper.showDownloadProgress(requireContext(), appName, getString(R.string.downloader_downloading, release.fileName))

        thread {
            try {
                val apkFile = File(requireContext().cacheDir, release.fileName)
                val connection = URL(release.downloadUrl).openConnection() as HttpURLConnection
                connection.connect()
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(apkFile)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) outputStream.write(buffer, 0, bytesRead)
                outputStream.close()
                inputStream.close()
                mainHandler.post { handleInstallResult(true, apkFile.absolutePath, developer, appId, appName) }
            } catch (e: Exception) {
                mainHandler.post { if (isAdded) handleInstallResult(false, getString(R.string.downloader_fail, e.message), developer, appId, appName) }
            }
        }
    }

    private fun handleInstallResult(success: Boolean, path: String?, developer: String, appId: String, appName: String) {
        if (!isAdded) return
        val safeContext = requireContext()
        if (success && path != null) {
            if (SettingsManager.isSilentInstallEnabled(safeContext)) {
                ShizukuInstaller.installApk(safeContext, path) { installSuccess, msg ->
                    if (!isAdded) return@installApk
                    Toast.makeText(safeContext, msg, Toast.LENGTH_LONG).show()
                    if (installSuccess) {
                        val packageInfo = safeContext.packageManager.getPackageArchiveInfo(path, 0)
                        val extractedPackageName = packageInfo?.packageName
                        if (extractedPackageName != null) {
                            AppCacheManager.savePackageName(safeContext, appId, extractedPackageName)
                            currentPackageName = extractedPackageName
                            setUIForOpenState(currentPackageName, developer, appId, appName)
                        } else setUIForInstallState(developer, appId, appName)
                        NotificationHelper.showStatusNotification(safeContext, appName, safeContext.getString(R.string.installer_success), true)
                    } else {
                        setUIForInstallState(developer, appId, appName)
                        NotificationHelper.showStatusNotification(safeContext, appName, safeContext.getString(R.string.installer_fail, msg), false)
                    }
                }
            } else {
                try {
                    val uri = FileProvider.getUriForFile(
                        safeContext,
                        "${safeContext.packageName}.fileprovider",
                        File(path)
                    )
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(installIntent)
                    setUIForInstallState(developer, appId, appName)
                } catch(e: Exception) {
                    Toast.makeText(safeContext, getString(R.string.store_error_generic), Toast.LENGTH_SHORT).show()
                    setUIForInstallState(developer, appId, appName)
                }
            }
        } else {
            Toast.makeText(safeContext, path ?: getString(R.string.store_error_generic), Toast.LENGTH_LONG).show()
            setUIForInstallState(developer, appId, appName)
            NotificationHelper.showStatusNotification(safeContext, appName, path ?: getString(R.string.store_download_failed), false)
        }
    }

    private fun uninstallApp(packageName: String, developer: String, appId: String, appName: String) {
        val safeContext = requireContext()
        binding.btnDetailsUninstall.isEnabled = false
        
        if (SettingsManager.isSilentInstallEnabled(safeContext)) {
            if (!ShizukuHelper.isShizukuRunning() || !ShizukuHelper.hasPermission()) {
                Toast.makeText(requireContext(), R.string.store_requesting_permission, Toast.LENGTH_SHORT).show()
                binding.btnDetailsUninstall.isEnabled = true
                return
            }
            ShizukuInstaller.uninstallApk(safeContext, packageName) { success, msg ->
                if (!isAdded) return@uninstallApk
                Toast.makeText(safeContext, msg, Toast.LENGTH_LONG).show()
                if (success) {
                    currentPackageName = ""
                    AppCacheManager.removePackageName(safeContext, appId)
                    setUIForInstallState(developer, appId, appName)
                    NotificationHelper.showStatusNotification(safeContext, appName, safeContext.getString(R.string.uninstall_success), true)
                } else {
                    binding.btnDetailsUninstall.isEnabled = true
                    NotificationHelper.showStatusNotification(safeContext, appName, safeContext.getString(R.string.uninstall_fail, msg), false)
                }
            }
        } else {
            try {
                val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
                startActivity(intent)
                binding.btnDetailsUninstall.isEnabled = true
            } catch (e: Exception) {
                Toast.makeText(safeContext, e.message, Toast.LENGTH_SHORT).show()
                binding.btnDetailsUninstall.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
