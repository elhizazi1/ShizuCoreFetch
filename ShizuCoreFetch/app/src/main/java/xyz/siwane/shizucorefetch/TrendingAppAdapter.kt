package xyz.siwane.shizucorefetch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import java.util.Locale

class TrendingAppAdapter(
    private val appList: List<AppModel>,
    private val onItemClick: (AppModel) -> Unit
) : RecyclerView.Adapter<TrendingAppAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivTrendingIcon)
        val tvName: TextView = view.findViewById(R.id.tvTrendingName)
        val tvRating: TextView = view.findViewById(R.id.tvTrendingRating)

        init {
            view.setOnClickListener {
                // استخدمنا adapterPosition بدلاً من absoluteAdapterPosition
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(appList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trending_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appItem = appList[position]
        
        holder.tvName.text = appItem.name
        holder.ivIcon.load(appItem.iconUrl) { crossfade(true) }

        // تحويل عدد النجوم إلى تقييم من 5
        val rating = calculateRatingFromStars(appItem.stars)
        holder.tvRating.text = if (rating > 0) String.format(Locale.US, "%.1f", rating) else "New"
    }

    override fun getItemCount(): Int = appList.size

    private fun calculateRatingFromStars(stars: Int): Float {
        return when {
            stars >= 10000 -> 4.9f
            stars >= 5000 -> 4.8f
            stars >= 1000 -> 4.6f
            stars >= 500 -> 4.4f
            stars >= 100 -> 4.1f
            stars >= 50 -> 3.8f
            stars >= 10 -> 3.5f
            stars > 0 -> 3.0f
            else -> 0.0f
        }
    }
}
