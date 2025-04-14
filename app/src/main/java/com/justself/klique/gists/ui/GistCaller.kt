package com.justself.klique.gists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.justself.klique.GlobalEventBus
import com.justself.klique.NetworkUtils
import com.justself.klique.gists.data.models.GistModel
import com.justself.klique.gists.ui.shared_composables.GistTile

@Composable
fun GistListCaller(
    modifier: Modifier = Modifier,
    gists: List<GistModel>,
    customerId: Int,
    onTap: ((GistModel) -> Unit),
    onHoldClick: ((String) -> Unit)? = null,
    defaultMessage: String? = null,
    listState: LazyListState
) {
    LaunchedEffect(gists) {
        GlobalEventBus.fetchGistBackground(gists)
    }
    val cachedMedia by GlobalEventBus.cachedMediaPaths.collectAsState()
    if (defaultMessage != null && gists.isEmpty()) {
        Text(
            text = defaultMessage,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
    } else {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = modifier
        ) {
            items(gists) { gist ->
                val mediaPaths = cachedMedia[gist.gistId]
                GistTile(
                    customerId,
                    onTap = {
                        onTap(gist)
                    },
                    onHoldClick = onHoldClick?.let { callback ->
                        { callback(gist.gistId) }
                    },
                    lastPostList = gist.lastGistComments.takeLast(3),
                    postImage = mediaPaths?.postImage,
                    postVideo = mediaPaths?.postVideo,
                    gist = gist
                )
            }
        }
    }
}