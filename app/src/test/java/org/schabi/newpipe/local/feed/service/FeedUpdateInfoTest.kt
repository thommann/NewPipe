package org.schabi.newpipe.local.feed.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.schabi.newpipe.database.subscription.SubscriptionEntity
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.Info
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.util.image.ImageStrategy

class FeedUpdateInfoTest {

    private fun createSubscription(
        uid: Long = 1L,
        avatarUrl: String? = "https://existing-avatar.com/img.jpg"
    ): SubscriptionEntity {
        return SubscriptionEntity(
            uid = uid,
            serviceId = 0,
            url = "https://example.com/test",
            name = "Sub Name",
            avatarUrl = avatarUrl,
            notificationMode = 0
        )
    }

    @Test
    fun `constructor with PlaylistInfo maps thumbnails to avatarUrl`() {
        val subscription = createSubscription()

        val playlistInfo = mock(PlaylistInfo::class.java)
        `when`(playlistInfo.name).thenReturn("Playlist Name")
        `when`(playlistInfo.url).thenReturn("https://example.com/playlist")
        `when`(playlistInfo.serviceId).thenReturn(0)
        `when`(playlistInfo.thumbnails).thenReturn(
            listOf(Image("https://thumb.com/img.jpg", 100, 100, Image.ResolutionLevel.MEDIUM))
        )
        `when`(playlistInfo.uploaderName).thenReturn("Uploader")
        `when`(playlistInfo.streamCount).thenReturn(42L)

        val feedUpdateInfo = FeedUpdateInfo(
            subscription,
            playlistInfo,
            emptyList(),
            emptyList()
        )

        assertEquals("Playlist Name", feedUpdateInfo.name)
        assertEquals(
            ImageStrategy.imageListToDbUrl(playlistInfo.thumbnails),
            feedUpdateInfo.avatarUrl
        )
        assertEquals("Uploader", feedUpdateInfo.description)
        assertEquals(42L, feedUpdateInfo.subscriberCount)
    }

    @Test
    fun `constructor with ChannelInfo maps avatars to avatarUrl`() {
        val subscription = createSubscription()

        val channelInfo = mock(ChannelInfo::class.java)
        `when`(channelInfo.name).thenReturn("Channel Name")
        `when`(channelInfo.url).thenReturn("https://example.com/channel")
        `when`(channelInfo.serviceId).thenReturn(0)
        `when`(channelInfo.avatars).thenReturn(
            listOf(Image("https://avatar.com/img.jpg", 100, 100, Image.ResolutionLevel.MEDIUM))
        )
        `when`(channelInfo.description).thenReturn("Channel description")
        `when`(channelInfo.subscriberCount).thenReturn(1000L)

        val feedUpdateInfo = FeedUpdateInfo(
            subscription,
            channelInfo,
            emptyList(),
            emptyList()
        )

        assertEquals("Channel Name", feedUpdateInfo.name)
        assertEquals(
            ImageStrategy.imageListToDbUrl(channelInfo.avatars),
            feedUpdateInfo.avatarUrl
        )
        assertEquals("Channel description", feedUpdateInfo.description)
        assertEquals(1000L, feedUpdateInfo.subscriberCount)
    }

    @Test
    fun `constructor with unknown Info type uses subscription avatar and null fields`() {
        val subscription = createSubscription(avatarUrl = "https://fallback.com/avatar.jpg")

        val unknownInfo = mock(Info::class.java)
        `when`(unknownInfo.name).thenReturn("Unknown Name")
        `when`(unknownInfo.url).thenReturn("https://example.com/unknown")
        `when`(unknownInfo.serviceId).thenReturn(0)

        val feedUpdateInfo = FeedUpdateInfo(
            subscription,
            unknownInfo,
            emptyList(),
            emptyList()
        )

        assertEquals("Unknown Name", feedUpdateInfo.name)
        assertEquals("https://fallback.com/avatar.jpg", feedUpdateInfo.avatarUrl)
        assertNull(feedUpdateInfo.description)
        assertNull(feedUpdateInfo.subscriberCount)
    }

    @Test
    fun `constructor preserves streams and errors`() {
        val subscription = createSubscription()

        val playlistInfo = mock(PlaylistInfo::class.java)
        `when`(playlistInfo.name).thenReturn("Playlist")
        `when`(playlistInfo.url).thenReturn("https://example.com/playlist")
        `when`(playlistInfo.serviceId).thenReturn(0)
        `when`(playlistInfo.thumbnails).thenReturn(emptyList())
        `when`(playlistInfo.uploaderName).thenReturn("Uploader")
        `when`(playlistInfo.streamCount).thenReturn(5L)

        val stream = mock(StreamInfoItem::class.java)
        val error = RuntimeException("test error")

        val feedUpdateInfo = FeedUpdateInfo(
            subscription,
            playlistInfo,
            listOf(stream),
            listOf(error)
        )

        assertEquals(1, feedUpdateInfo.streams.size)
        assertEquals(1, feedUpdateInfo.errors.size)
        assertEquals("test error", feedUpdateInfo.errors[0].message)
    }

    @Test
    fun `constructor maps uid and notificationMode from subscription`() {
        val subscription = createSubscription(uid = 99L)

        val playlistInfo = mock(PlaylistInfo::class.java)
        `when`(playlistInfo.name).thenReturn("Playlist")
        `when`(playlistInfo.url).thenReturn("https://example.com/playlist")
        `when`(playlistInfo.serviceId).thenReturn(0)
        `when`(playlistInfo.thumbnails).thenReturn(emptyList())
        `when`(playlistInfo.uploaderName).thenReturn("Uploader")
        `when`(playlistInfo.streamCount).thenReturn(0L)

        val feedUpdateInfo = FeedUpdateInfo(
            subscription,
            playlistInfo,
            emptyList(),
            emptyList()
        )

        assertEquals(99L, feedUpdateInfo.uid)
        assertEquals(0, feedUpdateInfo.notificationMode)
    }
}
