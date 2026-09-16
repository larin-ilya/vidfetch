package app.vidfetch.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.vidfetch.mobile.ui.theme.VidFetchTheme

/** Renders one screen inside the real app shell (background + bottom navigation). */
@Composable
fun AppPreview(route: String, dark: Boolean, screen: @Composable () -> Unit) {
    VidFetchTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = { VidFetchBottomBar(route) {} },
            ) { inner ->
                Box(Modifier.padding(inner)) { screen() }
            }
        }
    }
}
