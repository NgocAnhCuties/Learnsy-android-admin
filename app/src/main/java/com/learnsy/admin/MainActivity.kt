package com.learnsy.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.learnsy.admin.ui.AppRoot
import com.learnsy.admin.ui.theme.LearnsyTypography

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LearnsyAdminApp()
        }
    }
}

@Composable
fun LearnsyAdminApp() {
    MaterialTheme(typography = LearnsyTypography) {
        Surface(modifier = Modifier) {
            AppRoot()
        }
    }
}
