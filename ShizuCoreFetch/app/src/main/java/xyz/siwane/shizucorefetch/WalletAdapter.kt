package xyz.siwane.shizucorefetch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import xyz.siwane.shizucorefetch.databinding.ItemWalletApkBinding

class WalletAdapter(
    private var apkList: List<LocalApkModel>,
    private val onDeleteClick: (LocalApkModel) -> Unit,
    private val onShareClick: (LocalApkModel) -> Unit,
    private val onOpenClick: (LocalApkModel) -> Unit
) : RecyclerView.Adapter<WalletAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemWalletApkBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWalletApkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = apkList[position]
        with(holder.binding) {
            tvApkName.text = item.appName
            tvApkInfo.text = "${item.packageName} | ${item.versionName}"
            tvApkSize.text = item.fileSize

            // عرض الأيقونة المستخرجة أو الأيقونة الافتراضية
            if (item.icon != null) {
                ivApkIcon.setImageDrawable(item.icon)
            } else {
                ivApkIcon.setImageResource(R.drawable.ic_round)
            }

            btnDelete.setOnClickListener { onDeleteClick(item) }
            btnShare.setOnClickListener { onShareClick(item) }
            btnOpenExternal.setOnClickListener { onOpenClick(item) }
        }
    }

    override fun getItemCount(): Int = apkList.size

    fun updateData(newList: List<LocalApkModel>) {
        apkList = newList
        notifyDataSetChanged()
    }
}
