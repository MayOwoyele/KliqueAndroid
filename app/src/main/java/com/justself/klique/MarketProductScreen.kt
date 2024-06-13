package com.justself.klique

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import androidx.compose.ui.res.stringResource


@Composable
fun MarketProductsScreen(viewModel: ProductViewModel, navController: NavHostController, customerId: Int, marketId: Int, marketName: String) {
    LaunchedEffect(marketId) {
        viewModel.setMarketSpecificId(marketId)  // Fetch market-specific products
    }
    val productsResource by viewModel.marketProducts.observeAsState(Resource.Loading())
    Column(modifier = Modifier.padding(0.dp)) {
        Text(marketName, style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(10.dp))

        when (productsResource) {
            is Resource.Loading -> {
                CircularProgressIndicator()
                Text("Loading market products...", style = MaterialTheme.typography.bodyLarge)
            }
            is Resource.Success -> {
                val products = (productsResource as Resource.Success<List<Product>>).data
                if (products.isNullOrEmpty()) {
                    Text("No products found", style = MaterialTheme.typography.bodyLarge)
                } else {
                    MarketProductList(products, viewModel, { viewModel.loadMoreMarketProducts(marketId) }, navController, customerId)
                }
            }
            is Resource.Error -> {
                Text("Error: ${(productsResource as Resource.Error<List<Product>>).message}", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { viewModel.fetchProducts(marketId = marketId) }) {
                    Text("Retry")
                }
            }
            else -> Text("Unexpected state", style = MaterialTheme.typography.bodyLarge)
        }
    }
}


@Composable
fun MarketVideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    val baseUrl = stringResource(id = R.string.base_url) // Fetch the base URL from resources
    val completeVideoUrl = baseUrl + videoUrl
    val handler = remember { Handler(Looper.getMainLooper()) }
    val videoView = remember { VideoView(context).apply { setVideoPath(completeVideoUrl) } }
    val runnable = remember {
        object : Runnable {
            override fun run() {
                if (videoView.isPlaying) {
                    videoView.seekTo(0)
                    videoView.start()
                }
                handler.postDelayed(this, 2000) // Reschedule the runnable
            }
        }
    }
    Log.d("MarketVideoPlayer", "Complete video URL: $completeVideoUrl")

    AndroidView(
        factory = { videoView },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f), // Example: Setting an aspect ratio of 16:9
        update = { view ->
            view.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = false
                mediaPlayer.setVolume(0f, 0f)  // Muting the video
                mediaPlayer.start()
                handler.postDelayed(runnable, 2000) // Start looping every 2 seconds
            }
            view.setOnErrorListener { _, what, extra ->
                Log.e("MarketVideoPlayer", "Error playing video: what=$what, extra=$extra")
                true
            }
        }
    )

    // Proper placement of DisposableEffect
    DisposableEffect(Unit) {
        onDispose {
            videoView.stopPlayback()
            handler.removeCallbacks(runnable)  // Important to avoid memory leaks
        }
    }
}


@Composable
fun MarketProductList(products: List<Product>, viewModel: ProductViewModel, onLoadMore: () -> Unit, navController: NavHostController, customerId: Int) {
    LazyColumn {
        items(products) { product ->
            MarketProductItem(
                product = product,
                customerId = customerId,
                onLikeClicked = { productId, customerId ->
                    viewModel.likeProduct(productId, customerId)  // Assuming likeProduct exists in viewModel
                },
                viewModel = viewModel,
                navController = navController
            )
        }
        item {
            Button(onClick = onLoadMore) {
                Text("Load More")
            }
        }
    }
}

@Composable
fun MarketProductItem(
    product: Product,
    customerId: Int,
    onLikeClicked: (Int, Int) -> Unit,
    viewModel: ProductViewModel,
    navController: NavHostController
) {
    var expanded by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                product.shopName.let { shopName -> // Make sure shopName is safely called
                    // Box wrapping text based on its content length
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(0.dp, 8.dp, 8.dp, 0.dp)) // Rounded corners on the right side
                            .background(MaterialTheme.colorScheme.primary) // Background color
                            .padding(horizontal = 16.dp, vertical = 4.dp) // Padding around the text
                    ) {
                        Text(
                            text = "by $shopName",
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.background),
                            modifier = Modifier.clickable {
                                navController.navigate("shop_details/${product.shopId}")
                            }
                        )
                    }
                }
                if (product.videoUrl != null) {
                    MarketVideoPlayer(videoUrl = product.videoUrl)
                }
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "Price: ₦${product.price}",
                    style = MaterialTheme.typography.displayMedium.copy(color = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        isLiked = !isLiked
                        onLikeClicked(product.productId, customerId)
                    }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        text = "${product.likes ?: 0}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimary)
                    )
                }
                if (expanded) {
                    Text(
                        text = product.description.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary)
                    )
                    TextButton(onClick = { expanded = false }) {
                        Text("Less", color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text(
                        text = product.description.orEmpty().take(55) + "...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary)
                    )
                    TextButton(onClick = { expanded = true }) {
                        Text("More", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.addToCart(product)
                Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 100.dp)
        ) {
            Text("Add to Cart", color = MaterialTheme.colorScheme.onPrimary)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "View Comments",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { navController.navigate("product/${product.productId}") }
                .padding(8.dp),
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary)
        )
    }
}
