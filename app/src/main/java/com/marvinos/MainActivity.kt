package com.marvinos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.marvinos.ui.theme.MarvinTheme
import com.marvinos.ui.navigation.MarvinNavGraph
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for the entire MarvinOS UI.
 * Navigation between screens is handled by [MarvinNavGraph].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarvinTheme {
                MarvinNavGraph()
            }
        }
    }
}
