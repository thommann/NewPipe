package org.schabi.newpipe.database.subscription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.database.subscription.SubscriptionEntity.Companion.TYPE_CHANNEL
import org.schabi.newpipe.database.subscription.SubscriptionEntity.Companion.TYPE_PLAYLIST

class SubscriptionEntityTest {

    @Test
    fun `isPlaylist returns true for playlist entity type`() {
        val entity = SubscriptionEntity(entityType = TYPE_PLAYLIST)
        assertTrue(entity.isPlaylist())
    }

    @Test
    fun `isPlaylist returns false for channel entity type`() {
        val entity = SubscriptionEntity(entityType = TYPE_CHANNEL)
        assertFalse(entity.isPlaylist())
    }

    @Test
    fun `isPlaylist returns false for default entity`() {
        val entity = SubscriptionEntity()
        assertFalse(entity.isPlaylist())
    }

    @Test
    fun `default entity type is TYPE_CHANNEL`() {
        val entity = SubscriptionEntity()
        assertEquals(TYPE_CHANNEL, entity.entityType)
    }

    @Test
    fun `toPlaylistInfoItem maps fields correctly`() {
        val entity = SubscriptionEntity(
            serviceId = 0,
            url = "https://www.youtube.com/playlist?list=PLtest",
            name = "Test Playlist",
            subscriberCount = 42L,
            description = "Uploader Name",
            entityType = TYPE_PLAYLIST
        )

        val item = entity.toPlaylistInfoItem()

        assertEquals(0, item.serviceId)
        assertEquals("https://www.youtube.com/playlist?list=PLtest", item.url)
        assertEquals("Test Playlist", item.name)
        assertEquals(42L, item.streamCount)
        assertEquals("Uploader Name", item.uploaderName)
    }

    @Test
    fun `toPlaylistInfoItem uses -1 for null subscriberCount`() {
        val entity = SubscriptionEntity(
            serviceId = 0,
            url = "https://example.com/playlist",
            name = "Playlist",
            subscriberCount = null,
            entityType = TYPE_PLAYLIST
        )

        val item = entity.toPlaylistInfoItem()
        assertEquals(-1L, item.streamCount)
    }

    @Test
    fun `toPlaylistInfoItem maps null description to null uploaderName`() {
        val entity = SubscriptionEntity(
            serviceId = 0,
            url = "https://example.com/playlist",
            name = "Playlist",
            description = null,
            entityType = TYPE_PLAYLIST
        )

        val item = entity.toPlaylistInfoItem()
        assertNull(item.uploaderName)
    }

    @Test
    fun `toChannelInfoItem maps fields correctly`() {
        val entity = SubscriptionEntity(
            serviceId = 0,
            url = "https://www.youtube.com/channel/UCtest",
            name = "Test Channel",
            subscriberCount = 1000L,
            description = "Channel description",
            entityType = TYPE_CHANNEL
        )

        val item = entity.toChannelInfoItem()

        assertEquals(0, item.serviceId)
        assertEquals("https://www.youtube.com/channel/UCtest", item.url)
        assertEquals("Test Channel", item.name)
        assertEquals(1000L, item.subscriberCount)
        assertEquals("Channel description", item.description)
    }

    @Test
    fun `toChannelInfoItem uses -1 for null subscriberCount`() {
        val entity = SubscriptionEntity(
            serviceId = 0,
            url = "https://example.com/channel",
            name = "Channel",
            subscriberCount = null,
            entityType = TYPE_CHANNEL
        )

        val item = entity.toChannelInfoItem()
        assertEquals(-1L, item.subscriberCount)
    }

    @Test
    fun `TYPE_CHANNEL and TYPE_PLAYLIST are distinct values`() {
        assertEquals(0, TYPE_CHANNEL)
        assertEquals(1, TYPE_PLAYLIST)
    }
}
