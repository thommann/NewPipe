package org.schabi.newpipe.local.feed.service

import org.schabi.newpipe.database.subscription.NotificationMode
import org.schabi.newpipe.database.subscription.SubscriptionEntity
import org.schabi.newpipe.extractor.Info
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.util.image.ImageStrategy

/**
 * Instances of this class might stay around in memory for some time while fetching the feed,
 * because of [FeedLoadManager.BUFFER_COUNT_BEFORE_INSERT]. Therefore this class should contain
 * as little data as possible to avoid out of memory errors. In particular, avoid storing whole
 * [ChannelInfo] objects, as they might contain raw JSON info in ready channel tabs link handlers.
 */
data class FeedUpdateInfo(
    val uid: Long,
    @NotificationMode
    val notificationMode: Int,
    val name: String,
    val avatarUrl: String?,
    val url: String,
    val serviceId: Int,
    // description and subscriberCount are null if the constructor info is from the fast feed method
    val description: String?,
    val subscriberCount: Long?,
    val streams: List<StreamInfoItem>,
    val errors: List<Throwable>
) {
    constructor(
        subscription: SubscriptionEntity,
        info: Info,
        streams: List<StreamInfoItem>,
        errors: List<Throwable>
    ) : this(
        uid = subscription.uid,
        notificationMode = subscription.notificationMode,
        name = info.name,
        avatarUrl = when (info) {
            is ChannelInfo -> ImageStrategy.imageListToDbUrl(info.avatars)
            is PlaylistInfo -> ImageStrategy.imageListToDbUrl(info.thumbnails)
            else -> subscription.avatarUrl
        },
        url = info.url,
        serviceId = info.serviceId,
        description = when (info) {
            is ChannelInfo -> info.description
            is PlaylistInfo -> info.uploaderName
            else -> null
        },
        subscriberCount = when (info) {
            is ChannelInfo -> info.subscriberCount
            is PlaylistInfo -> info.streamCount
            else -> null
        },
        streams = streams,
        errors = errors
    )

    /**
     * Integer id, can be used as notification id, etc.
     */
    val pseudoId: Int
        get() = url.hashCode()

    lateinit var newStreams: List<StreamInfoItem>
}
