package com.justself.klique.gists.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.justself.klique.Screen
import com.justself.klique.getUserOverlayColor
import com.justself.klique.gists.data.models.GistModel
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import kotlinx.coroutines.launch

object HomeScreens {
    val lazyListState = LazyListState(
        firstVisibleItemIndex = 0,
        firstVisibleItemScrollOffset = 0
    )
    val latestVerticalState = LazyListState(
        firstVisibleItemIndex = 0,
        firstVisibleItemScrollOffset = 0
    )
    val interactionsVerticalState = LazyListState(
        firstVisibleItemIndex = 0,
        firstVisibleItemScrollOffset = 0
    )
}

@Composable
fun GistScreen(customerId: Int, viewModel: SharedCliqueViewModel) {
    var selectedGist by remember { mutableStateOf<GistModel?>(null) }
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = HomeScreens.lazyListState
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val currentPage by remember {
        derivedStateOf { HomeScreens.lazyListState.firstVisibleItemIndex }
    }
    val topBarHeight = 80.dp
    val topBarHeightPx = with(LocalDensity.current) { topBarHeight.roundToPx().toFloat() }
    val topBarOffset = remember { mutableFloatStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val deltaY = available.y
                val oldOffset = topBarOffset.floatValue
                val newOffset = (oldOffset + deltaY).coerceIn(-topBarHeightPx, 0f)
                val consumedY = newOffset - oldOffset
                topBarOffset.floatValue = newOffset
                return Offset(x = 0f, y = consumedY)
            }
        }
    }
    val animatedTopBarHeight by animateDpAsState(
        targetValue = topBarHeight + with(LocalDensity.current) { topBarOffset.floatValue.toDp() },
        animationSpec = tween(durationMillis = 100)
    )
    LaunchedEffect(currentPage) {
        when (currentPage) {
            0 -> viewModel.fetchTrendingGists(customerId)
            1 -> viewModel.fetchInteractions(customerId)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedTopBarHeight)
                    .clipToBounds()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentPage == 0) MaterialTheme.colorScheme.primary else Color.Gray
                        ),
                        modifier = Modifier
                            .padding(16.dp)
                            .width(146.dp)
                    ) {
                        Text(text = "Latests", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch { lazyListState.animateScrollToItem(1) }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentPage == 1) MaterialTheme.colorScheme.primary else Color.Gray
                        ),
                        modifier = Modifier
                            .padding(16.dp)
                            .width(146.dp)
                    ) {
                        Text(text = "Interactions", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            LazyRow(
                state = lazyListState,
                flingBehavior = snapFlingBehavior,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .width(screenWidth)
                            .fillMaxHeight()
                    ) {
                        val message = "Probably loading"
                        GistListCaller(
                            modifier = Modifier.nestedScroll(nestedScrollConnection),
                            uiState.trendingGists,
                            customerId,
                            onTap = { gist -> selectedGist = gist },
                            defaultMessage = message,
                            listState = HomeScreens.latestVerticalState,
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .width(screenWidth)
                            .fillMaxHeight()
                    ) {
                        val message = "You don't yet have any interactions with any gists"
                        GistListCaller(
                            modifier = Modifier.nestedScroll(nestedScrollConnection),
                            gists = uiState.interactions,
                            customerId,
                            onTap = { gist -> selectedGist = gist },
                            defaultMessage = message,
                            listState = HomeScreens.interactionsVerticalState,
                        )
                    }
                }
            }
        }
        selectedGist?.let { theGist ->
            val onGistStart = {
                viewModel.enterGist(theGist.gistId)
            }
            GistPreview(theGist, onDismiss = { selectedGist = null }, onGistStart)
        }
    }
}

@Composable
fun GistPreview(selectedGist: GistModel, onDismiss: () -> Unit, onGistStart: () -> Unit) {
    val randomSeed by remember { mutableIntStateOf((0..10000).random()) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .clickable(enabled = false) { }
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.6f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = selectedGist.topic,
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val listState = rememberLazyListState()
                    Box(Modifier.fillMaxHeight(0.85f)) {
                        Column {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                state = listState,
                                reverseLayout = true
                            ) {
                                items(selectedGist.lastGistComments.reversed()) { comment ->
                                    val bubbleColor = lerp(
                                        Color.Gray,
                                        getUserOverlayColor(comment.userId, randomSeed),
                                        0.2f
                                    )
                                    val chatBubbleShape = GenericShape { size, _ ->
                                        val tailWidth = 20f
                                        val tailHeight = 10f
                                        val cornerRadius = 16f
                                        moveTo(cornerRadius, 0f)
                                        lineTo(size.width - cornerRadius, 0f)
                                        arcTo(
                                            rect = Rect(
                                                left = size.width - 2 * cornerRadius,
                                                top = 0f,
                                                right = size.width,
                                                bottom = 2 * cornerRadius
                                            ),
                                            startAngleDegrees = -90f,
                                            sweepAngleDegrees = 90f,
                                            forceMoveTo = false
                                        )
                                        lineTo(
                                            size.width,
                                            size.height - cornerRadius - tailHeight
                                        )
                                        arcTo(
                                            rect = Rect(
                                                left = size.width - 2 * cornerRadius,
                                                top = size.height - 2 * cornerRadius - tailHeight,
                                                right = size.width,
                                                bottom = size.height - tailHeight
                                            ),
                                            startAngleDegrees = 0f,
                                            sweepAngleDegrees = 90f,
                                            forceMoveTo = false
                                        )
                                        lineTo(
                                            cornerRadius + tailWidth,
                                            size.height - tailHeight
                                        )
                                        lineTo(tailWidth, size.height)
                                        lineTo(tailWidth, size.height - tailHeight)
                                        arcTo(
                                            rect = Rect(
                                                left = 0f,
                                                top = size.height - 2 * cornerRadius - tailHeight,
                                                right = 2 * cornerRadius,
                                                bottom = size.height - tailHeight
                                            ),
                                            startAngleDegrees = 90f,
                                            sweepAngleDegrees = 90f,
                                            forceMoveTo = false
                                        )
                                        lineTo(0f, cornerRadius)
                                        arcTo(
                                            rect = Rect(
                                                left = 0f,
                                                top = 0f,
                                                right = 2 * cornerRadius,
                                                bottom = 2 * cornerRadius
                                            ),
                                            startAngleDegrees = 180f,
                                            sweepAngleDegrees = 90f,
                                            forceMoveTo = false
                                        )
                                        close()
                                    }
                                    Surface(
                                        shape = chatBubbleShape,
                                        color = bubbleColor,
                                        modifier = Modifier.padding(
                                            vertical = 4.dp,
                                            horizontal = 8.dp
                                        )
                                    ) {
                                        Text(
                                            text = "${comment.senderName}: ${comment.comment}",
                                            modifier = Modifier.padding(8.dp),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onGistStart()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Enter Gist",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}