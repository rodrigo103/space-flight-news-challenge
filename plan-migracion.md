# Plan de migración: navigation3 → Navigation Compose

## Archivos a modificar (5 archivos + 2 dependencias)

| Archivo | Acción |
|---|---|
| `gradle/libs.versions.toml` | Agregar `navigation-compose`, remover navigation3 |
| `app/build.gradle.kts` | Reemplazar dependencias de navigation3 |
| `NavigationKeys.kt` → `Routes.kt` | Renombrar y redefinir rutas |
| `Navigation.kt` | Reescribir con NavHost + NavController |
| `ArticleDetailViewModel.kt` | Simplificar (sin cambios funcionales) |
| `ArticleDetailScreen.kt` | Simplificar (sin cambios funcionales) |

## Paso a paso

### 1. Dependencias

**libs.versions.toml:**
- Agregar `navVersion = "2.9.0"`
- Agregar `androidx-navigation-compose`
- Remover `nav3Core`, `lifecycleViewmodelNav3`
- Remover `androidx-navigation3-runtime`, `androidx-navigation3-ui`, `androidx-lifecycle-viewmodel-navigation3`

**build.gradle.kts:**
- Remover:
  - `implementation(libs.androidx.navigation3.ui)`
  - `implementation(libs.androidx.navigation3.runtime)`
  - `implementation(libs.androidx.lifecycle.viewmodel.navigation3)`
- Agregar:
  - `implementation(libs.androidx.navigation.compose)`

### 2. Routes (reemplaza NavigationKeys)

```kotlin
@Serializable data object ArticlesRoute
@Serializable data class DetailRoute(val articleId: Int)
```

### 3. Navigation.kt

Reemplazar `NavDisplay` + `rememberNavBackStack` + `entry<T>` por:

```kotlin
val navController = rememberNavController()
NavHost(navController = navController, startDestination = ArticlesRoute) {
    composable<ArticlesRoute> {
        ArticlesListScreen(onArticleClick = { articleId ->
            navController.navigate(DetailRoute(articleId))
        })
    }
    composable<DetailRoute> { backStackEntry ->
        val detail = backStackEntry.toRoute<DetailRoute>()
        ArticleDetailScreen(
            articleId = detail.articleId,
            onBack = { navController.popBackStack() }
        )
    }
}
```

### 4. Pantallas

Sin cambios. `ArticleDetailScreen` ya recibe `articleId: Int` y `onBack`.
`ArticlesListScreen` ya recibe `onArticleClick`.

### 5. ViewModels

Sin cambios. `ArticleDetailViewModel` ya recibe `articleId` por constructor (factory manual en el screen).

---

## Impacto

- **0 cambios** en lógica de negocio, repositorios, data layer
- **0 cambios** en pantallas
- **0 cambios** en ViewModels
- Solo se tocan archivos de navegación y dependencias
