package xyz.siwane.shizucorefetch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import io.noties.markwon.Markwon

data class CommentModel(
    val id: Long,
    val username: String,
    val avatarUrl: String,
    val body: String,
    val isMyComment: Boolean,
    val canDelete: Boolean,
    val reactionsText: String
)

class CommentAdapter(
    private val commentsList: List<CommentModel>,
    private val markwon: Markwon,
    private val onReplyClick: (String) -> Unit,
    private val onEditClick: (Long, String) -> Unit,
    private val onDeleteClick: (Long) -> Unit,
    private val onReactClick: (Long) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivCommentAvatar)
        val tvUsername: TextView = view.findViewById(R.id.tvCommentUsername)
        val tvBody: TextView = view.findViewById(R.id.tvCommentBody)
        val tvReactions: TextView = view.findViewById(R.id.tvCommentReactions)
        val tvReply: TextView = view.findViewById(R.id.tvCommentReply)
        val tvEdit: TextView = view.findViewById(R.id.tvCommentEdit)
        val tvDelete: TextView = view.findViewById(R.id.tvCommentDelete)
        val ivAddReaction: ImageView = view.findViewById(R.id.ivCommentAddReaction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentsList[position]

        holder.tvUsername.text = comment.username
        markwon.setMarkdown(holder.tvBody, comment.body)

        holder.ivAvatar.load(comment.avatarUrl) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }

        if (comment.reactionsText.isNotEmpty()) {
            holder.tvReactions.visibility = View.VISIBLE
            holder.tvReactions.text = comment.reactionsText
        } else {
            holder.tvReactions.visibility = View.GONE
        }

        holder.tvReply.setOnClickListener { onReplyClick(comment.username) }
        holder.ivAddReaction.setOnClickListener { onReactClick(comment.id) }

        if (comment.isMyComment) {
            holder.tvEdit.visibility = View.VISIBLE
            holder.tvEdit.setOnClickListener { onEditClick(comment.id, comment.body) }
        } else {
            holder.tvEdit.visibility = View.GONE
        }

        if (comment.canDelete) {
            holder.tvDelete.visibility = View.VISIBLE
            holder.tvDelete.setOnClickListener { onDeleteClick(comment.id) }
        } else {
            holder.tvDelete.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = commentsList.size
}
