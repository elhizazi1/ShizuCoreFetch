package xyz.siwane.shizucorefetch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import xyz.siwane.shizucorefetch.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var storeAdapter: StoreAdapter
    private lateinit var featuredAdapter: FeaturedAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupSearchView()
        setupSwipeRefresh()
        
        fetchAppsFromNetwork(isManualRefresh = false)
    }

    override fun onResume() {
        super.onResume()
        if (::storeAdapter.isInitialized) {
            storeAdapter.refreshCache(requireContext())
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshHome.setColorSchemeResources(
            androidx.appcompat.R.color.abc_btn_colored_borderless_text_material,
            androidx.appcompat.R.color.abc_primary_text_material_light
        )
        
        binding.swipeRefreshHome.setOnRefreshListener {
            binding.layoutRealContent.visibility = View.GONE
            binding.layoutDummyContent.visibility = View.VISIBLE
            fetchAppsFromNetwork(isManualRefresh = true)
        }
    }

    private fun setupRecyclerViews() {
        binding.rvFeaturedApps.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvFeaturedApps.setHasFixedSize(true)

        binding.rvAllApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllApps.isNestedScrollingEnabled = false
    }

    private fun setupSearchView() {
        binding.toolbarHome.inflateMenu(R.menu.menu_home)
        val searchItem = binding.toolbarHome.menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.queryHint = getString(R.string.store_search_hint)

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                binding.root.post {
                    binding.tvFeaturedTitle.visibility = View.GONE
                    binding.rvFeaturedApps.visibility = View.GONE
                    binding.tvAllAppsTitle.visibility = View.GONE
                }
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                binding.root.post {
                    if (::featuredAdapter.isInitialized && featuredAdapter.itemCount > 0) {
                        binding.tvFeaturedTitle.visibility = View.VISIBLE
                        binding.rvFeaturedApps.visibility = View.VISIBLE
                    }
                    binding.tvAllAppsTitle.visibility = View.VISIBLE
                    
                    if (::storeAdapter.isInitialized) {
                        storeAdapter.filter("")
                    }
                }
                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { return false }
            override fun onQueryTextChange(newText: String?): Boolean {
                binding.root.post {
                    if (::storeAdapter.isInitialized) {
                        storeAdapter.filter(newText ?: "")
                    }
                }
                return true
            }
        })
    }

    private fun fetchAppsFromNetwork(isManualRefresh: Boolean) {
        if (!isManualRefresh) {
            val cachedApps = StoreCacheManager.getCachedApps(requireContext())
            if (cachedApps != null && cachedApps.isNotEmpty()) {
                displayApps(cachedApps)
            } else {
                binding.layoutRealContent.visibility = View.GONE
                binding.layoutDummyContent.visibility = View.VISIBLE
                if (!binding.swipeRefreshHome.isRefreshing) {
                    binding.swipeRefreshHome.isRefreshing = true
                }
            }
        }

        RetrofitClient.instance.getApps(Constants.GAS_URL).enqueue(object : Callback<List<AppModel>> {
            override fun onResponse(call: Call<List<AppModel>>, response: Response<List<AppModel>>) {
                if (!isAdded) return 
                
                binding.swipeRefreshHome.isRefreshing = false
                
                if (response.isSuccessful && response.body() != null) {
                    val newApps = response.body()!!
                    
                    if (isManualRefresh || StoreCacheManager.getCachedApps(requireContext()) != newApps) {
                        StoreCacheManager.saveApps(requireContext(), newApps)
                        displayApps(newApps)
                    }
                } else {
                    handleFetchError(isManualRefresh)
                }
            }

            override fun onFailure(call: Call<List<AppModel>>, t: Throwable) {
                if (!isAdded) return
                binding.swipeRefreshHome.isRefreshing = false
                handleFetchError(isManualRefresh)
                
                val cachedApps = StoreCacheManager.getCachedApps(requireContext())
                if (cachedApps == null) {
                    val errorMsg = "${getString(R.string.store_fetch_error_network)} ${t.message}"
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun handleFetchError(isManualRefresh: Boolean) {
        val cachedApps = StoreCacheManager.getCachedApps(requireContext())
        if (cachedApps != null && cachedApps.isNotEmpty()) {
            displayApps(cachedApps)
            if (isManualRefresh) {
                Toast.makeText(requireContext(), getString(R.string.store_fetch_error_network), Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.layoutDummyContent.visibility = View.GONE
        }
    }

    private fun displayApps(apps: List<AppModel>) {
        if (!isAdded || _binding == null) return
        
        binding.layoutDummyContent.visibility = View.GONE
        binding.layoutRealContent.visibility = View.VISIBLE

        storeAdapter = StoreAdapter(apps) { appItem, packageName ->
            openAppDetails(appItem, packageName)
        }
        binding.rvAllApps.adapter = storeAdapter

        val featuredApps = apps.filter { it.hasJsonStore }
        
        if (featuredApps.isEmpty()) {
            binding.tvFeaturedTitle.visibility = View.GONE
            binding.rvFeaturedApps.visibility = View.GONE
        } else {
            binding.tvFeaturedTitle.visibility = View.VISIBLE
            binding.rvFeaturedApps.visibility = View.VISIBLE
            
            featuredAdapter = FeaturedAdapter(featuredApps) { appItem ->
                openAppDetails(appItem, "")
            }
            binding.rvFeaturedApps.adapter = featuredAdapter
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
            adApproved = appItem.adApproved,
            bannerUrl = appItem.bannerUrl,
            bannerUrlAr = appItem.bannerUrlAr,
            downloads = appItem.downloads,      // تمرير عدد التنزيلات
            category = appItem.category,        // تمرير الفئة
            categoryAr = appItem.categoryAr     // تمرير الفئة بالعربية
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
