package xyz.siwane.shizucorefetch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import xyz.siwane.shizucorefetch.databinding.ItemFeaturedAppBinding
import java.util.Locale

class FeaturedAdapter(
    private val featuredList: List<AppModel>,
    private val onAppClick: (AppModel) -> Unit
) : RecyclerView.Adapter<FeaturedAdapter.FeaturedViewHolder>() {

    inner class FeaturedViewHolder(val binding: ItemFeaturedAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedViewHolder {
        val binding = ItemFeaturedAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeaturedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeaturedViewHolder, position: Int) {
        val appItem = featuredList[position]
        val context = holder.itemView.context

        // 1. تعيين اسم التطبيق (تم إزالة الوصف بناءً على طلبك)
        holder.binding.tvFeaturedAppName.text = appItem.name

        // 2. معالجة الفئة (Category) بناءً على لغة النظام
        val currentLang = Locale.getDefault().language
        val finalCategory = if (currentLang == "ar" && appItem.categoryAr.isNotEmpty()) appItem.categoryAr else appItem.category
        
        if (finalCategory.isNotEmpty()) {
            holder.binding.cvFeaturedCategory.visibility = View.VISIBLE
            holder.binding.tvFeaturedCategory.text = finalCategory
        } else {
            holder.binding.cvFeaturedCategory.visibility = View.GONE
        }

        // 3. تنسيق ذكي ومختصر للنجوم (لجعلها قصيرة وجذابة مثل 1.5K)
        val stars = appItem.stars
        val formattedStars = when {
            stars >= 1_000_000 -> String.format(Locale.US, "%.1fM", stars / 1_000_000.0)
            stars >= 1_000 -> String.format(Locale.US, "%.1fK", stars / 1_000.0)
            else -> stars.toString()
        }
        holder.binding.tvFeaturedStars.text = formattedStars

        // 4. تنسيق ذكي للتنزيلات (معتمد على مواردك لتجنب النصوص الثابتة)
        val formattedDownloads = when {
            appItem.downloads >= 1_000_000 -> context.getString(R.string.store_downloads_million, String.format(Locale.US, "%.1f", appItem.downloads / 1_000_000.0))
            appItem.downloads >= 1_000 -> context.getString(R.string.store_downloads_thousand, String.format(Locale.US, "%.1f", appItem.downloads / 1_000.0))
            else -> appItem.downloads.toString()
        }
        
        if (appItem.downloads > 0) {
            holder.binding.cvFeaturedDownloads.visibility = View.VISIBLE
            holder.binding.tvFeaturedDownloads.text = formattedDownloads
        } else {
            holder.binding.cvFeaturedDownloads.visibility = View.GONE
        }
        
        // 5. تحديد اتجاه تخطيط الجهاز لاختيار البانر المناسب
        val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val targetBannerUrl = if (isRtl && appItem.bannerUrlAr.isNotEmpty()) {
            appItem.bannerUrlAr
        } else if (appItem.bannerUrl.isNotEmpty()) {
            appItem.bannerUrl
        } else {
            appItem.iconUrl
        }

        // 6. تحميل الصورة وإرسال النقرة
        holder.binding.ivFeaturedBanner.load(targetBannerUrl) {
            crossfade(true)
        }

        holder.binding.root.setOnClickListener {
            onAppClick(appItem)
        }
    }

    override fun getItemCount(): Int = featuredList.size
}
