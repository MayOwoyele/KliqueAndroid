package com.justself.klique.gists.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.justself.klique.databinding.ReplyingLayoutBinding
import com.justself.klique.gists.viewHolder.GistPageViewHolder
import com.justself.klique.nav.NavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class InnerCarouselAdapter(
    override val items: MutableList<TimelineItem> = mutableListOf(),
    private var footerHeight: Int = 0,
    private val downloadMap: StateFlow<Map<String, File>>,
    val context: Context,
    private val clickAction: (Int, () -> Unit) -> Unit,
    private val scope: CoroutineScope,
    val nav: NavigationManager
) : Adapter<RecyclerView.ViewHolder>(), GistCarouselAdapter {
    companion object {
        private const val TYPE_GIST = 0
        private const val TYPE_FOOTER = 99
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_GIST -> {
                val binding = ReplyingLayoutBinding.inflate(inflater, parent, false)
                GistPageViewHolder(binding, clickAction, scope, nav, context, items)
            }

            TYPE_FOOTER -> {
                val emptyView = View(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        footerHeight
                    )
                }
                FooterViewHolder(emptyView)
            }

            else -> error("Unsupported view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items.getOrNull(position) ?: return

        when {
            holder is GistPageViewHolder && item is TimelineItem.GistPost -> {
                val file = downloadMap.value[item.post.postId]
                holder.bind(item.post, file)
            }

            else -> {
                // no-op or log unexpected holder/item pair
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position == items.size) return TYPE_FOOTER

        return when (items[position]) {
            is TimelineItem.GistPost -> TYPE_GIST
        }
    }

    override fun getItemCount(): Int = items.size + 1
    fun addItems(newItems: List<TimelineItem>) {
        val filtered = newItems.filter { new ->
            items.none { existing -> existing.id == new.id }
        }
        val start = items.size
        items.addAll(filtered)
        notifyItemRangeInserted(start, filtered.size)
    }
    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is PlayableViewHolder) {
            holder.pauseMedia()
        }
    }

    override fun setFooterHeight(height: Int) {
        footerHeight = height
    }
}