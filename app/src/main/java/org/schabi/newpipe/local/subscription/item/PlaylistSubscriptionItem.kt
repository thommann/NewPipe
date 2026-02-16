package org.schabi.newpipe.local.subscription.item

import android.widget.ImageView
import android.widget.TextView
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import org.schabi.newpipe.R
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.util.Localization
import org.schabi.newpipe.util.OnClickGesture
import org.schabi.newpipe.util.image.PicassoHelper

class PlaylistSubscriptionItem(
    val infoItem: PlaylistInfoItem,
    private val subscriptionId: Long = -1L,
    var itemVersion: ItemVersion = ItemVersion.MINI,
    var gesturesListener: OnClickGesture<PlaylistInfoItem>? = null
) : Item<GroupieViewHolder>() {
    override fun getId(): Long = if (subscriptionId == -1L) super.getId() else subscriptionId

    enum class ItemVersion { MINI, GRID }

    override fun getLayout(): Int = when (itemVersion) {
        ItemVersion.MINI -> R.layout.list_playlist_mini_item
        ItemVersion.GRID -> R.layout.list_playlist_grid_item
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val itemTitleView = viewHolder.root.findViewById<TextView>(R.id.itemTitleView)
        val itemStreamCountView = viewHolder.root.findViewById<TextView>(R.id.itemStreamCountView)
        val itemUploaderView = viewHolder.root.findViewById<TextView>(R.id.itemUploaderView)
        val itemThumbnailView = viewHolder.root.findViewById<ImageView>(R.id.itemThumbnailView)

        itemTitleView.text = infoItem.name
        itemUploaderView.text = infoItem.uploaderName

        if (infoItem.streamCount >= 0) {
            itemStreamCountView.text =
                Localization.localizeStreamCountMini(viewHolder.root.context, infoItem.streamCount)
        } else {
            itemStreamCountView.text = ""
        }

        PicassoHelper.loadPlaylistThumbnail(infoItem.thumbnails).into(itemThumbnailView)

        gesturesListener?.run {
            viewHolder.root.setOnClickListener { selected(infoItem) }
            viewHolder.root.setOnLongClickListener {
                held(infoItem)
                true
            }
        }
    }

    override fun getSpanSize(spanCount: Int, position: Int): Int {
        return if (itemVersion == ItemVersion.GRID) 1 else spanCount
    }
}
