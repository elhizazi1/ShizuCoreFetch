package xyz.siwane.shizucorefetch

import android.Manifest
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.siwane.shizucorefetch.databinding.FragmentWalletBinding
import java.io.File
import java.util.Locale

class WalletFragment : Fragment() {

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!
    private lateinit var walletAdapter: WalletAdapter
    
    private var pendingApkModel: LocalApkModel? = null

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pendingApkModel?.let { exportAndOpenFolder(it) }
            pendingApkModel = null
        } else {
            Toast.makeText(requireContext(), "صلاحية التخزين مطلوبة لحفظ التطبيق", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarWallet.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupRecyclerView()
        loadLocalApks()
    }

    private fun setupRecyclerView() {
        binding.rvWalletApks.layoutManager = LinearLayoutManager(requireContext())
        walletAdapter = WalletAdapter(
            apkList = emptyList(),
            onDeleteClick = { item -> confirmDeletion(item) },
            onShareClick = { item -> shareApkFile(item) },
            onOpenClick = { item -> openInExternalEditor(item) }
        )
        binding.rvWalletApks.adapter = walletAdapter
    }

    private fun loadLocalApks() {
        // تحديث المسار ليتطابق مع ApkDownloader (يبحث في المسار الخارجي أولاً)
        val cacheDir = requireContext().externalCacheDir ?: requireContext().cacheDir
        val apkFiles = cacheDir.listFiles { _, name -> name.endsWith(".apk") } ?: emptyArray()
        
        val pm = requireContext().packageManager
        val localList = mutableListOf<LocalApkModel>()

        for (file in apkFiles) {
            try {
                val packageInfo = pm.getPackageArchiveInfo(file.absolutePath, 0)
                if (packageInfo != null) {
                    val appInfo = packageInfo.applicationInfo
                    
                    if (appInfo != null) {
                        appInfo.sourceDir = file.absolutePath
                        appInfo.publicSourceDir = file.absolutePath
                        
                        val appName = pm.getApplicationLabel(appInfo).toString()
                        val packageName = packageInfo.packageName ?: ""
                        val versionName = packageInfo.versionName ?: "1.0"
                        val sizeInMb = String.format(Locale.US, "%.2f MB", file.length().toDouble() / (1024 * 1024))
                        
                        // 💡 التعديل هنا: جلب أيقونة الـ APK الحقيقية
                        val apkIcon = appInfo.loadIcon(pm)

                        // تمرير الأيقونة للكلاس LocalApkModel
                        localList.add(LocalApkModel(file, appName, packageName, versionName, sizeInMb, apkIcon))
                    }
                }
            } catch (e: Exception) {
            }
        }

        walletAdapter.updateData(localList)
        binding.tvEmptyWallet.visibility = if (localList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDeletion(item: LocalApkModel) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.wallet_delete_title))
            .setMessage(getString(R.string.wallet_delete_msg, item.appName))
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ ->
                if (item.file.delete()) {
                    loadLocalApks()
                    Toast.makeText(requireContext(), getString(R.string.wallet_delete_success), Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun shareApkFile(item: LocalApkModel) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                item.file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.wallet_share_title)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.wallet_error_action), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openInExternalEditor(item: LocalApkModel) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pendingApkModel = item
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }
        }
        exportAndOpenFolder(item)
    }

    private fun exportAndOpenFolder(item: LocalApkModel) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val shizuDir = File(downloadsDir, "ShizuCoreFetch")
            
            if (!shizuDir.exists()) {
                shizuDir.mkdirs()
            }

            val publicApkFile = File(shizuDir, item.file.name)
            if (!publicApkFile.exists() || publicApkFile.length() != item.file.length()) {
                item.file.copyTo(publicApkFile, overwrite = true)
                Toast.makeText(requireContext(), "تم حفظ التطبيق في مسار التنزيلات", Toast.LENGTH_SHORT).show()
            }

            val builder = android.os.StrictMode.VmPolicy.Builder()
            android.os.StrictMode.setVmPolicy(builder.build())

            var folderOpened = false
            val pm = requireContext().packageManager
            val dirUri = Uri.fromFile(shizuDir)

            val trustedFileManagers = listOf(
                "bin.mt.plus",                 
                "ru.zdevs.zarchiver",          
                "ru.zdevs.zarchiver.pro",      
                "com.mixplorer",               
                "pl.solidexplorer2",           
                "com.speedsoftware.rootexplorer",
                "com.google.android.apps.nbu.files", 
                "com.android.documentsui"      
            )

            for (pkg in trustedFileManagers) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(dirUri, "*/*") 
                    intent.setPackage(pkg)               
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    
                    if (intent.resolveActivity(pm) != null) {
                        startActivity(intent)
                        folderOpened = true
                        break
                    }
                } catch (e: Exception) {}
            }

            if (!folderOpened) {
                try {
                    val uriSystem = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FShizuCoreFetch")
                    val intentSystem = Intent(Intent.ACTION_VIEW)
                    intentSystem.setDataAndType(uriSystem, "vnd.android.document.directory") 
                    intentSystem.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    
                    if (intentSystem.resolveActivity(pm) != null) {
                        startActivity(intentSystem)
                        folderOpened = true
                    }
                } catch (e: Exception) {}
            }

            if (!folderOpened) {
                val mimeTypes = arrayOf("resource/folder", "vnd.android.cursor.dir/*", "inode/directory")
                for (mime in mimeTypes) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(dirUri, mime)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        
                        if (intent.resolveActivity(pm) != null) {
                            startActivity(Intent.createChooser(intent, "اختر مدير الملفات"))
                            folderOpened = true
                            break
                        }
                    } catch (e: Exception) {}
                }
            }

            if (!folderOpened) {
                val intentDownloads = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                intentDownloads.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intentDownloads)
            }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.wallet_error_action), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
