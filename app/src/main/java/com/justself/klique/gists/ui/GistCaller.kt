package com.justself.klique.gists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.justself.klique.GlobalEventBus
import com.justself.klique.NetworkUtils
import com.justself.klique.gists.data.models.GistModel
import com.justself.klique.gists.ui.shared_composables.GistTile

@Composable
fun GistListCaller(
    gists: List<GistModel>,
    customerId: Int,
    onTap: ((String) -> Unit),
    onHoldClick: ((String) -> Unit)? = null,
    defaultMessage: String? = null
) {
    LaunchedEffect(gists) {
        GlobalEventBus.fetchGistBackground(gists)
    }

    val cachedMedia by GlobalEventBus.cachedMediaPaths.collectAsState()
    val listState = rememberLazyListState()
    if (defaultMessage != null && gists.isEmpty()) {
        Text(
            text = defaultMessage,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
    } else {
        LazyColumn(state= listState, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            items(gists) { gist ->
                val newImage = gist.image?.let { NetworkUtils.fixLocalHostUrl(it) }
                val mediaPaths = cachedMedia[gist.gistId]
                GistTile(
                    customerId,
                    gist.topic,
                    gist.description,
                    newImage,
                    gist.activeSpectators,
                    onTap = {
                        onTap(gist.gistId)
                    },
                    onHoldClick = onHoldClick?.let { callback ->
                        { callback(gist.gistId) }
                    },
                    lastPostList = gist.lastGistComments,
                    postImage = mediaPaths?.postImage,
                    postVideo = mediaPaths?.postVideo
                )
            }
        }
    }
}