package com.justself.klique

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun NetworkImage(modifier: Modifier = Modifier, imageUrl: String) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUrl) {
        Log.d("NetworkImage", "Loading image from URL: $imageUrl")
        bitmap = loadImage(imageUrl)
        if (bitmap == null) {
            Log.e("NetworkImage", "Failed to load image from URL: $imageUrl")
        }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
                .size(150.dp) // Adjust size as needed
                .clip(CircleShape), // Add clipping after sizing
            contentScale = ContentScale.Crop
        )

    }
}
suspend fun loadImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val responseData = NetworkUtils.resourceRequest(url, "GET", emptyMap())
        // Use the ByteArray directly with BitmapFactory
        BitmapFactory.decodeStream(responseData.inputStream())
    } catch (e: Exception) {
        Log.e("loadImage", "Error loading image: ${e.message}")
        null
    }
}

@Composable
fun MarketsScreen(navController: NavHostController) {
    val viewModel: MarketViewModel = viewModel()
    val markets = viewModel.markets.value

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(markets) { market ->
            MarketItem(market, navController)
        }
    }
}

@Composable
fun MarketItem(market: Market, navController: NavHostController) {
    val imageUrl = stringResource(id = R.string.base_url) + market.profilePhoto
    val encodedMarketName = Uri.encode(market.marketName)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("marketProduct/${market.marketId}/${encodedMarketName}") }
            .padding(16.dp)
            .height(75.dp), // Adjust the height as needed
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            modifier = Modifier.size(60.dp) // Adjust size as needed
        ) {
            NetworkImage(
                imageUrl = imageUrl,
                modifier = Modifier.clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = market.marketName, style = MaterialTheme.typography.displayLarge)
            Text(text = market.location, style = MaterialTheme.typography.bodyLarge)
        }
    }

}
