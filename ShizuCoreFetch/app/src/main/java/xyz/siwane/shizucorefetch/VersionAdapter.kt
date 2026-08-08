package xyz.siwane.shizucorefetch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import xyz.siwane.shizucorefetch.databinding.ItemVersionBinding

class VersionAdapter(
    private val releaseList: List<ReleaseModel>,
    private val onVersionClick: (ReleaseModel) -> Unit
) : RecyclerView.Adapter<VersionAdapter.VersionViewHolder>() {

    inner class VersionViewHolder(val binding: ItemVersionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VersionViewHolder {
        val binding = ItemVersionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VersionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VersionViewHolder, position: Int) {
        val release = releaseList[position]
        
        holder.binding.tvVersionName.text = release.versionName
        holder.binding.tvVersionDate.text = release.publishedAt
        
        holder.binding.btnInstallVersion.setOnClickListener {
            onVersionClick(release)
        }
    }

    override fun getItemCount(): Int = releaseList.size
}
