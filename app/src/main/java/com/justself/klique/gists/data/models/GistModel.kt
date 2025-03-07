package com.justself.klique.gists.data.models

import com.justself.klique.gists.ui.shared_composables.LastGistComments

// TODO: Make adjustments according to Api
data class GistModel(
    val gistId: String = "",
    val topic: String,
    val description: String,
    val image: String,
    val activeSpectators: Int,
    val gistType: String = "Public",
    val lastGistComments: List<LastGistComments>
)