package com.justself.klique

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter

@Composable
fun ChatRoomsCategory(
    navController: NavController,
    campuses: Boolean = false,
    interests: Boolean = false,
    viewModel: ChatRoomsCategoryViewModel = viewModel()
) {
    val campusesCategories = viewModel.campusesCategories.collectAsState().value
    val interestsCategories = viewModel.interestsCategories.collectAsState().value
    LaunchedEffect(Unit) {
        viewModel.fetchCategories()
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Log.d("Triggered", "Triggered")
        if (campuses) {
            campusesCategories.forEach { category ->
                CategoryItem(category = category, navController, viewModel)
            }
        }
        if (interests) {
            interestsCategories.forEach { category ->
                CategoryItem(category = category, navController, viewModel)
            }
        }
    }
}

@Composable
fun CategoryItem(category: ChatRoomCategory, navController: NavController, viewModel: ChatRoomsCategoryViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp).clickable {
                navController.navigate("categoryOptions/${category.categoryId}") },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(category.categoryImage),
            contentDescription = category.categoryName,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = category.categoryName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}