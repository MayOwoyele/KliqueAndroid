package com.justself.klique

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Groups2
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter

@Composable
fun LeftDrawer(drawerState: MutableState<Boolean>, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = drawerState.value,
        enter = slideInHorizontally { -it } + fadeIn(), // Slide in from off-screen left
        exit = slideOutHorizontally { -it }, // Slide out to the off-screen left
        modifier = modifier
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = if (isSystemInDarkTheme()) 0.8f else 0.2f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                drawerState.value = false
            }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = 80 / 100f)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Consume the click */ }
            ) {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { /*TODO*/
                    }) {
                        Icon(
                            imageVector = if (isSystemInDarkTheme()) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Toggle Dark Mode "
                        )
                    }
                }
                LeftDrawerItem(
                    modifier = Modifier.padding(bottom = 16.dp),
                    leading = {
                        Surface(
                            modifier = Modifier
                                .size((60).dp)
                                .clip(CircleShape.copy(CornerSize(150.dp)))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = "https://unsplash.com/photos/MP0IUfwrn0A/download?force=true&w=640"),
                                contentScale = ContentScale.Crop,
                                contentDescription = "Logo"
                            )
                        }
                    },
                    text = "Tatiana Manois",
                    secondaryText = "+2341234567890",
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Wallet, contentDescription = "Profile")
                            Text(text = "500KC")
                        }

                    }
                )
                LeftDrawerItem(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                    leading = { Icon(Icons.Rounded.Person, contentDescription = "Profile") },
                    text = "My Profile",
                )
                LeftDrawerItem(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                    leading = {
                        Icon(
                            Icons.Rounded.MarkChatUnread,
                            contentDescription = "Profile"
                        )
                    },
                    text = "Chatrooms",
                )
                LeftDrawerItem(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                    leading = { Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = "Direct Messages") },
                    text = "Direct Messages",
                )
                /*Button(onClick = { drawerState.value = false }) {
                    Text("Close Drawer")
                }*/
            }
        }
    }
}

@Composable
fun LeftDrawerItem(
    modifier: Modifier,
    leading: @Composable (modifier: Modifier) -> Unit = {},
    text: String,
    secondaryText: String? = null,
    trailing: @Composable (modifier: Modifier) -> Unit = {},
    onClick: () -> Unit = {}
) {
    Surface(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading(
                Modifier
                    .weight(1f)
                    .clip(CircleShape.copy())
            )
            Column(
                Modifier.weight(5f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(text = text, style = MaterialTheme.typography.titleMedium)
                if (secondaryText != null) {
                    Text(text = secondaryText, style = MaterialTheme.typography.bodyMedium)
                }
            }
            trailing(Modifier.weight(1f))


        }
    }
}

@Composable
fun RightDrawer(
    drawerState: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    productViewModel: ProductViewModel = viewModel()
) {
    AnimatedVisibility(
        visible = drawerState.value,
        enter = slideInHorizontally { it }, // Slide in from the right
        exit = slideOutHorizontally { it }, // Slide out to the right
        modifier = modifier
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                drawerState.value = false
            }
            .background(color = Color.Black.copy(alpha = if (isSystemInDarkTheme()) 0.8f else 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {

                    }
                    .fillMaxWidth(80 / 100f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(16.dp)
                ) {
                    Text("Cart", style = MaterialTheme.typography.displayLarge)
                    CartItemsList(productViewModel)
                }
                Button(
                    onClick = {
                        // Trigger checkout functionality
                        handleCheckout(productViewModel)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = productViewModel.cartItems.observeAsState().value?.isNotEmpty() == true
                ) {
                    Text("Checkout", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

fun handleCheckout(viewModel: ProductViewModel) {
    // Logic to initiate checkout process
    println("Proceeding to checkout...")
}

@Composable
fun CartItemsList(productViewModel: ProductViewModel = viewModel()) {
    val cartItemsState: List<Product> =
        productViewModel.cartItems.observeAsState(initial = emptyList()).value

    LazyColumn {
        items(cartItemsState) { product ->
            CartItemRow(product, productViewModel)
        }
    }
}

@Composable
fun CartItemRow(product: Product, viewModel: ProductViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = product.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Price: ₦${product.price}", style = MaterialTheme.typography.displayMedium)
            Text(text = "Quantity: ${product.quantity}", style = MaterialTheme.typography.bodyLarge)
        }
        Button(
            onClick = { viewModel.removeFromCart(product.productId) },
            modifier = Modifier
                .padding(8.dp)
                .widthIn(max = 100.dp)
                .height(36.dp)
        ) {
            Text("Remove", style = MaterialTheme.typography.bodyMedium)
        }
    }
}