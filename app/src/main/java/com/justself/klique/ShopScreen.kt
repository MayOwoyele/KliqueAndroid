package com.justself.klique

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.VideoView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ShopOwnerScreen(shopId: Int, navController: NavHostController, productViewModel: ProductViewModel) {
    val viewModel: ShopViewModel = viewModel()
    val currentPage by remember { mutableIntStateOf(1) }
    val shopResource by viewModel.shopDetails.observeAsState()

    LaunchedEffect(shopId, currentPage) {
        viewModel.fetchShopDetails(shopId, currentPage)
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Main content of the screen
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp)  // Space for the arrow, adjust as needed
        ) {
            when (val resource = shopResource) {
                is Resource.Loading -> CircularProgressIndicator()
                is Resource.Success -> resource.data?.let { shop ->
                    ShopInfoHeader(shop)  // Composable for displaying the shop's header info
                    Button(onClick = {
                        navController.navigate("dmScreen/${shop.ownerId}/${shop.shopName}")
                    }) {
                        Text("DM Shop")
                    }
                    ShopDetails(shop, navController, productViewModel)  // Composable for displaying products
                }
                is Resource.Error -> Text("Error: ${resource.message ?: "Unknown error"}")
                else -> Text("Unexpected Error")
            }
        }

        // Positioning the back arrow at the top z-index
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 0.dp)  // Optional padding to adjust the position
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
    }
}



@Composable
fun ShopInfoHeader(shop: Shop) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Profile picture section
        shop.profilePhoto.let {
            val image = loadImageBitmap(it)
            image?.let { img ->
                Image(
                    bitmap = img,
                    contentDescription = "Shop Profile",
                    modifier = Modifier
                        .size(150.dp)  // Specify the size of the image
                        .clip(CircleShape)  // Clip the image to a circle
                        .border(2.dp, Color.Gray, CircleShape)  // Apply a border to the circle
                )
            } ?: Text("Loading image...")  // Show a placeholder or loading text if the image is null
        }

        // Text information section
        Column(modifier = Modifier
            .padding(start = 16.dp)
            .align(Alignment.CenterVertically)
        ) {
            Text(
                text = "Shop: ${shop.shopName}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${shop.shopDescription.take(60)}${if (shop.shopDescription.length > 60) "..." else ""}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Address: ${shop.address}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


@Composable
fun loadImageBitmap(url: String): ImageBitmap? {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    val context = LocalContext.current
    val baseUrl = stringResource(id = R.string.base_url)

    LaunchedEffect(url) {
        try {
            val data = withContext(Dispatchers.IO) {
                NetworkUtils.resourceRequest(baseUrl + url)
            }
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            image = bitmap.asImageBitmap()
        } catch (e: Exception) {
            // Log or handle exception
        }
    }

    return image
}

@Composable
fun ShopDetails(shop: Shop, navController: NavHostController, productViewModel: ProductViewModel) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(shop.products) { product ->
            ProductCard(product, navController, productViewModel)
        }
    }
}


@Composable
fun ProductCard(product: Product, navController: NavHostController, productViewModel: ProductViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }  // Local state to manage like status
    val context = LocalContext.current  // Get the current context

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (product.videoUrl != null) {
                    VideoPlayer(videoUrl = product.videoUrl)
                }
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp)  // Add padding if necessary
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "Price: ₦${product.price}",
                    style = MaterialTheme.typography.displayMedium.copy(color = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp)  // Control padding to align text
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        // Display message instead of toggling the like status
                        Toast.makeText(context, "Product can only be liked within the market", Toast.LENGTH_LONG).show()
                    }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        text = "${product.likes}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimary)
                    )
                }
                val description = product.description.orEmpty()
                if (description.length > 55) {
                    if (expanded) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary)
                        )
                        TextButton(onClick = { expanded = false }) {
                            Text("Less", color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Text(
                            text = description.take(55) + "...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary)
                        )
                        TextButton(onClick = { expanded = true }) {
                            Text("More", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                productViewModel.addToCart(product)
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
@Composable
fun VideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    val baseUrl = stringResource(id = R.string.base_url) // Fetch the base URL from resources
    val completeVideoUrl = baseUrl + videoUrl
    val handler = remember { Handler(Looper.getMainLooper()) }
    val videoView = remember {
        VideoView(context).apply {
            setVideoPath(completeVideoUrl)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
            )
        }
    }
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
                Log.e("VideoPlayer", "Error playing video: what=$what, extra=$extra")
                true
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            videoView.stopPlayback()
            handler.removeCallbacks(runnable)  // Important to avoid memory leaks
        }
    }
}