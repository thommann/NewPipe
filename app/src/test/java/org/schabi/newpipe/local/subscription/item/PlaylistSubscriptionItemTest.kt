package org.schabi.newpipe.local.subscription.item

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.schabi.newpipe.R
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.local.subscription.item.PlaylistSubscriptionItem.ItemVersion

class PlaylistSubscriptionItemTest {

    private fun createInfoItem(
        serviceId: Int = 0,
        url: String = "https://www.youtube.com/playlist?list=PLtest",
        name: String = "Test Playlist"
    ): PlaylistInfoItem {
        return PlaylistInfoItem(serviceId, url, name)
    }

    @Test
    fun `getId returns subscriptionId when set`() {
        val item = PlaylistSubscriptionItem(createInfoItem(), subscriptionId = 42L)
        assertEquals(42L, item.id)
    }

    @Test
    fun `getId returns super id when subscriptionId is -1`() {
        val item = PlaylistSubscriptionItem(createInfoItem(), subscriptionId = -1L)
        // super.getId() is based on object identity, just verify it doesn't return -1
        assertNotEquals(-1L, item.id)
    }

    @Test
    fun `getLayout returns mini layout for MINI version`() {
        val item = PlaylistSubscriptionItem(createInfoItem(), itemVersion = ItemVersion.MINI)
        assertEquals(R.layout.list_playlist_mini_item, item.layout)
    }

    @Test
    fun `getLayout returns grid layout for GRID version`() {
        val item = PlaylistSubscriptionItem(createInfoItem(), itemVersion = ItemVersion.GRID)
        assertEquals(R.layout.list_playlist_grid_item, item.layout)
    }

    @Test
    fun `getSpanSize returns 1 for GRID version`() {
        val item = PlaylistSubscriptionItem(createInfoItem(), itemVersion = ItemVersion.GRID)
        assertEquals(1, item.getSpanSize(3, 0))
        assertEquals(1, item.getSpanSize(4, 0))
    }

    @Test
    fun `getSpanSize returns spanCount for MINI version`() {
        val item = PlaylistSubscriptionItem(createInfoItem(), itemVersion = ItemVersion.MINI)
        assertEquals(3, item.getSpanSize(3, 0))
        assertEquals(4, item.getSpanSize(4, 0))
        assertEquals(1, item.getSpanSize(1, 0))
    }

    @Test
    fun `default itemVersion is MINI`() {
        val item = PlaylistSubscriptionItem(createInfoItem())
        assertEquals(ItemVersion.MINI, item.itemVersion)
    }

    @Test
    fun `itemVersion can be changed after construction`() {
        val item = PlaylistSubscriptionItem(createInfoItem(), itemVersion = ItemVersion.MINI)
        assertEquals(R.layout.list_playlist_mini_item, item.layout)

        item.itemVersion = ItemVersion.GRID
        assertEquals(R.layout.list_playlist_grid_item, item.layout)
    }

    @Test
    fun `infoItem is accessible`() {
        val infoItem = createInfoItem(name = "My Playlist")
        val item = PlaylistSubscriptionItem(infoItem)
        assertEquals("My Playlist", item.infoItem.name)
    }
}
