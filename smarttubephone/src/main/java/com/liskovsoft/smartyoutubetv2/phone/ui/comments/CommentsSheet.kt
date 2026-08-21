package com.liskovsoft.smartyoutubetv2.phone.ui.comments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.liskovsoft.mediaserviceinterfaces.data.CommentItem
import com.liskovsoft.sharedutils.rx.RxHelper
import com.liskovsoft.smartyoutubetv2.phone.R
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class CommentsSheet(private val activity: android.app.Activity) {
    private var disposable: Disposable? = null

    fun show(commentsKey: String?) {
        if (commentsKey.isNullOrBlank()) {
            android.widget.Toast.makeText(activity, R.string.comments_unavailable, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(R.layout.sheet_comments, null)
        val list = view.findViewById<RecyclerView>(R.id.comments_list)
        val progress = view.findViewById<View>(R.id.comments_progress)
        val empty = view.findViewById<TextView>(R.id.comments_empty)
        val adapter = CommentAdapter()
        list.layoutManager = LinearLayoutManager(activity)
        list.adapter = adapter
        dialog.setContentView(view)
        dialog.setOnDismissListener { RxHelper.disposeActions(disposable) }
        dialog.show()

        disposable = YouTubeServiceManager.instance().commentsService
            .getCommentsObserve(commentsKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ group ->
                progress.visibility = View.GONE
                val comments = group?.comments ?: emptyList()
                adapter.submit(comments)
                empty.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE
            }, {
                progress.visibility = View.GONE
                empty.visibility = View.VISIBLE
                empty.setText(R.string.comments_unavailable)
            })
    }
}

private class CommentAdapter : RecyclerView.Adapter<CommentAdapter.Holder>() {
    private val items = mutableListOf<CommentItem>()

    fun submit(values: List<CommentItem>) {
        items.clear()
        items.addAll(values.filter { !it.isEmpty })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.author.text = item.authorName ?: ""
        holder.message.text = item.message ?: ""
        holder.meta.text = listOfNotNull(item.publishedDate, item.likeCount).joinToString("  •  ")
        Glide.with(holder.avatar).load(item.authorPhoto).circleCrop().into(holder.avatar)
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.comment_avatar)
        val author: TextView = view.findViewById(R.id.comment_author)
        val message: TextView = view.findViewById(R.id.comment_message)
        val meta: TextView = view.findViewById(R.id.comment_meta)
    }
}
