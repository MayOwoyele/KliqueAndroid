package com.justself.klique.gists.ui

import com.justself.klique.gists.data.models.GistModel

data class GistUiState(
    val trendingGists: List<GistModel>,
    val myGists: List<GistModel>
)
