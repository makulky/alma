package com.alma.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alma.app.ui.navigation.AlmaNavHost
import com.alma.app.ui.theme.AlmaTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    // Alma solo está en español: así también lo están los textos de Material, como el selector de fechas.
    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag("es-ES"))
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlmaTheme {
                AlmaNavHost()
            }
        }
    }
}
