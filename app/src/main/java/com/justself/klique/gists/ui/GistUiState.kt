package com.justself.klique.gists.ui

import com.justself.klique.gists.data.models.GistModel

data class GistUiState(
    val trendingGists: List<GistModel> = generateGistModels(),
    val myGists: List<GistModel> = generateGistModels(true)
)

fun generateGistModels(reverse: Boolean = false): List<GistModel> {
    val profileImageUrls = listOf(
        "https://unsplash.com/photos/WLUHO9A_xik/download?force=true&w=640",
        "https://unsplash.com/photos/Mv9hjnEUHR4/download?force=true&w=640",
        "https://unsplash.com/photos/aZjw7xI3QAA/download?force=true&w=640",
        "https://unsplash.com/photos/b1Hg7QI-zcc/download?force=true&w=640",
        "https://unsplash.com/photos/5P91SF0zNsI/download?force=true&w=640",
        "https://unsplash.com/photos/u3ajSXhZM_U/download?force=true&w=640",
        "https://unsplash.com/photos/vB5qtt8X4NA/download?force=true&w=640",
        "https://unsplash.com/photos/Y8lCoTRgHPE/download?force=true&w=640",
        "https://unsplash.com/photos/Igct8iZucFI/download?force=true&w=640",
        "https://unsplash.com/photos/_7LbC5J-jw4/download?force=true&w=640",
        "https://unsplash.com/photos/Ti7L8Q4CWag/download?force=true&w=640",
        "https://unsplash.com/photos/RN6ts8IZ4_0/download?force=true&w=640",
        "https://unsplash.com/photos/FV3GConVSss/download?force=true&w=640",
        "https://unsplash.com/photos/CFz5dvgUmCw/download?force=true&w=640",
        "https://unsplash.com/photos/gKXKBY-C-Dk/download?force=true&w=640",
        "https://unsplash.com/photos/MP0IUfwrn0A/download?force=true&w=640",
        "https://unsplash.com/photos/x9I-6yoXrXE/download?force=true&w=640",
        "https://unsplash.com/photos/KdeqA3aTnBY/download?force=true&w=640",
        "https://unsplash.com/photos/mEZ3PoFGs_k/download?force=true&w=640",
        "https://unsplash.com/photos/PhYq704ffdA/download?force=true&w=640"
    )
    return profileImageUrls.indices.map { index ->
        GistModel(
            gistId = "gist$index",
            topic = "Gist $index Samuel bassey john, A  lowly developer  looking for money in this economy",
            description = "Description $index The linux can also isolate user resources from each other by using a feature called Multiuser Operating System. which allows access to independent resources like memory, CPU, RAM and Applications.\n" +
                    "Android built a feature on this technology called ‘Application Sandbox’.",
            image = if (reverse) profileImageUrls.reversed()[index] else profileImageUrls[index],
            activeSpectators = 10,
        )
    }
}
