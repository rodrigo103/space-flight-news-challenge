package com.example.myandroidapp.ui.articles.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.example.myandroidapp.domain.model.Article

@Stable
interface ArticleCardSettings {
    val article: Article
    val onClick: () -> Unit
    val modifier: Modifier

    @Composable
    operator fun invoke(
        modifier: Modifier = Modifier,
    ) = ArticleCard(
        article = article,
        onClick = onClick,
        modifier = this.modifier.then(modifier),
    )
}

fun articleCardSettings(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
): ArticleCardSettings = ArticleCardSettingsData(
    article = article,
    onClick = onClick,
    modifier = modifier,
)

private class ArticleCardSettingsData(
    override val article: Article,
    override val onClick: () -> Unit,
    override val modifier: Modifier,
) : ArticleCardSettings
