package com.example.myandroidapp.ui.articles.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.example.myandroidapp.domain.model.Article

@Stable
interface ArticleDetailContentSettings {
    val article: Article
    val modifier: Modifier

    @Composable
    operator fun invoke(
        modifier: Modifier = Modifier,
    ) = ArticleDetailContent(
        article = article,
        modifier = this.modifier.then(modifier),
    )
}

fun articleDetailContentSettings(
    article: Article,
    modifier: Modifier = Modifier,
): ArticleDetailContentSettings = ArticleDetailContentSettingsData(
    article = article,
    modifier = modifier,
)

private class ArticleDetailContentSettingsData(
    override val article: Article,
    override val modifier: Modifier,
) : ArticleDetailContentSettings
