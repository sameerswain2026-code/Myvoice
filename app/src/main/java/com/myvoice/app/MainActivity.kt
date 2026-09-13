package com.myvoice.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.myvoice.app.ui.AppNavHost
import com.myvoice.app.ui.theme.MyVoiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MyVoiceApp).container
        setContent {
            MyVoiceTheme {
                AppNavHost(container)
            }
        }
    }
}
