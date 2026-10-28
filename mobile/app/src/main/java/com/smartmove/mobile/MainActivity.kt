package com.smartmove.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smartmove.mobile.ui.AppRoot
import com.smartmove.mobile.ui.theme.SmartMoveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SmartMoveApplication).container
        setContent {
            SmartMoveTheme {
                AppRoot(container)
            }
        }
    }
}
