package com.justself.klique.gists.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.justself.klique.gists.data.models.GistModel
import com.justself.klique.gists.ui.shared_composables.GistTile
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel

@Composable
fun MyGists(myGists: List<GistModel>, customerId: Int, viewModel: SharedCliqueViewModel) {
    if (myGists.isEmpty()) {
        Text(
            text = "You haven't created a gist. Click on the plus icon below to do so. Note that you can't own more than 5 gists",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            items(myGists) { gist ->
                GistTile(
                    gist.gistId,
                    customerId,
                    gist.topic,
                    gist.description,
                    gist.image,
                    gist.activeSpectators,
                    onTap = { viewModel.enterGist(gist.gistId)},
                    onHoldClick = {
                        viewModel.floatGist(gist.gistId)
                        Log.d("Float Gist", "Gist floated")
                    }
                )
            }
        }
    }
}
