package com.justself.klique

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LeftDrawer(drawerState: MutableState<Boolean>, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = drawerState.value,
        enter = slideInHorizontally { -it }, // Slide in from off-screen left
        exit = slideOutHorizontally { -it }, // Slide out to the off-screen left
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(350.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume the click */ }
        ) {
            Text("Categories", style = MaterialTheme.typography.displayLarge)
            Button(onClick = { drawerState.value = false }) {
                Text("Close Drawer")
            }
        }
    }
}

@Composable
fun RightDrawer(drawerState: MutableState<Boolean>, modifier: Modifier = Modifier, productViewModel: ProductViewModel = viewModel()) {
    AnimatedVisibility(
        visible = drawerState.value,
        enter = slideInHorizontally { it }, // Slide in from the right
        exit = slideOutHorizontally { it }, // Slide out to the right
        modifier = modifier
    ) {
        Box(modifier = Modifier
            .fillMaxHeight()
            .width(350.dp)
            .background(MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier
                .matchParentSize()
                .padding(16.dp)) {
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
fun handleCheckout(viewModel: ProductViewModel) {
    // Logic to initiate checkout process
    println("Proceeding to checkout...")
}
@Composable
fun CartItemsList(productViewModel: ProductViewModel = viewModel()) {
    val cartItemsState: List<Product> = productViewModel.cartItems.observeAsState(initial = emptyList()).value

    LazyColumn {
        items(cartItemsState) { product ->
            CartItemRow(product, productViewModel)
        }
    }
}

@Composable
fun CartItemRow(product: Product, viewModel: ProductViewModel) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = product.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Price: ₦${product.price}", style = MaterialTheme.typography.displayMedium)
            Text(text = "Quantity: ${product.quantity}", style = MaterialTheme.typography.bodyLarge)
        }
        Button(
            onClick = { viewModel.removeFromCart(product.productId) },
            modifier = Modifier.padding(8.dp).widthIn(max = 100.dp).height(36.dp)
        ) {
            Text("Remove", style = MaterialTheme.typography.bodyMedium)
        }
    }
}