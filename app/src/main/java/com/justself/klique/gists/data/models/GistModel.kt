package com.justself.klique.gists.data.models

import com.justself.klique.GistType
import com.justself.klique.gists.ui.shared_composables.LastGistComments


data class GistModel(
    val gistId: String = "",
    val topic: String,
    val description: String,
    val image: String?,
    val activeSpectators: Int,
    val gistType: GistType = GistType.Public,
    val lastGistComments: List<LastGistComments>,
    val postImage: String? = null,
    val postVideo: String? = null,
)