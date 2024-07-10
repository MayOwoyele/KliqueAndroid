package com.justself.klique.gists.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.justself.klique.gists.data.models.GistModel
import com.justself.klique.gists.ui.shared_composables.GistTile

@Composable
fun MyGists(myGists: List<GistModel>, customerId: Int){
        LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            items(myGists) { gist ->
                GistTile(gist.gistId, customerId, gist.title, gist.description, gist.image, gist.activeSpectators, onTap = {})
            }
        }
}
