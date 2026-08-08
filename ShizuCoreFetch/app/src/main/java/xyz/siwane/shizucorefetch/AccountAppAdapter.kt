package xyz.siwane.shizucorefetch

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import xyz.siwane.shizucorefetch.databinding.ItemAccountAppBinding

class AccountAppAdapter(
    private val appList: List<AppModel>,
    private val isUpdateList: Boolean, // true = قائمة التحديثات، false = قائمة المثبتة
    private val onAppClick: (AppModel) -> Unit,
    private val onActionClick: (AppModel) -> Unit
) : RecyclerView.Adapter<AccountAppAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAccountAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAccountAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appItem = appList[position]
        val context = holder.itemView.context

        with(holder.binding) {
            tvAppName.text = appItem.name
            
            ivAppIcon.load(appItem.iconUrl) {
                crossfade(true)
                // يمكن إضافة صورة افتراضية في حالة فشل التحميل
                // error(R.drawable.ic_placeholder) 
            }

            // تغيير نص ولون الزر بناءً على نوع القائمة
            if (isUpdateList) {
                btnAction.text = context.getString(R.string.update_button)
                // لتمييز التحديثات، سنجعل الزر بلون مختلف قليلاً (اللون الأساسي)
                btnAction.setTextColor(context.getColor(R.color.colorPrimary))
            } else {
                btnAction.text = context.getString(R.string.open_button)
                // لون ثانوي لزر الفتح
                btnAction.setTextColor(context.getColor(androidx.appcompat.R.color.material_grey_600))
            }

            // عند النقر على الزر (فتح أو تحديث)
            btnAction.setOnClickListener {
                onActionClick(appItem)
            }

            // عند النقر على البطاقة بالكامل (للذهاب لصفحة التفاصيل)
            root.setOnClickListener {
                onAppClick(appItem)
            }
        }
    }

    override fun getItemCount(): Int = appList.size
}
