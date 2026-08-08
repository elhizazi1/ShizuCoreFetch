package xyz.siwane.shizucorefetch

import android.content.pm.PackageManager
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
import rikka.shizuku.Shizuku
import xyz.siwane.shizucorefetch.databinding.FragmentStoreBinding

class StoreFragment : Fragment(), Shizuku.OnRequestPermissionResultListener {

    private var _binding: FragmentStoreBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var storeAdapter: StoreAdapter
    private val SHIZUKU_REQUEST_CODE = 1001

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuPermission()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Shizuku.addRequestPermissionResultListener(this)
        Shizuku.addBinderReceivedListener(binderReceivedListener)

        setupRecyclerView()
        setupSearchView()
        setupSwipeRefresh()
        
        fetchAppsFromNetwork(isManualRefresh = false)
        
        if (ShizukuHelper.isShizukuRunning()) {
            checkShizukuPermission()
        } else {
            binding.rvApps.postDelayed({
                if (isAdded && !ShizukuHelper.isShizukuRunning()) {
                    Toast.makeText(requireContext(), getString(R.string.store_shizuku_not_running), Toast.LENGTH_LONG).show()
                }
            }, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::storeAdapter.isInitialized) {
            storeAdapter.refreshCache(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Shizuku.removeRequestPermissionResultListener(this)
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        _binding = null
    }

    private fun checkShizukuPermission() {
        if (ShizukuHelper.hasPermission()) {
            if (isAdded) {
                Toast.makeText(requireContext(), getString(R.string.store_shizuku_permission_granted), Toast.LENGTH_SHORT).show()
            }
        } else {
            ShizukuHelper.requestPermission(SHIZUKU_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (!isAdded) return
        if (requestCode == SHIZUKU_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), getString(R.string.store_shizuku_permission_granted), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.store_shizuku_permission_denied), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshStore.setColorSchemeResources(
            androidx.appcompat.R.color.abc_btn_colored_borderless_text_material,
            androidx.appcompat.R.color.abc_primary_text_material_light
        )
        
        binding.swipeRefreshStore.setOnRefreshListener {
            binding.rvApps.visibility = View.GONE
            binding.layoutDummyStore.visibility = View.VISIBLE
            fetchAppsFromNetwork(isManualRefresh = true)
        }
    }

    private fun setupRecyclerView() {
        binding.rvApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApps.setHasFixedSize(true) 
    }

    private fun setupSearchView() {
        binding.toolbarStore.inflateMenu(R.menu.menu_store)
        
        val searchItem = binding.toolbarStore.menu.findItem(R.id.action_search_store)
        val searchView = searchItem.actionView as SearchView

        searchView.queryHint = getString(R.string.store_search_hint)

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                binding.root.post {
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
                binding.rvApps.visibility = View.GONE
                binding.layoutDummyStore.visibility = View.VISIBLE
                if (!binding.swipeRefreshStore.isRefreshing) {
                    binding.swipeRefreshStore.isRefreshing = true
                }
            }
        }

        RetrofitClient.instance.getApps(Constants.GAS_URL).enqueue(object : Callback<List<AppModel>> {
            override fun onResponse(call: Call<List<AppModel>>, response: Response<List<AppModel>>) {
                if (!isAdded) return
                
                binding.swipeRefreshStore.isRefreshing = false
                
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
                binding.swipeRefreshStore.isRefreshing = false
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
            binding.layoutDummyStore.visibility = View.GONE
        }
    }

    private fun displayApps(apps: List<AppModel>) {
        if (!isAdded || _binding == null) return
        
        binding.layoutDummyStore.visibility = View.GONE
        binding.rvApps.visibility = View.VISIBLE
        
        storeAdapter = StoreAdapter(apps) { appItem, packageName ->
            openAppDetails(appItem, packageName)
        }
        binding.rvApps.adapter = storeAdapter
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
