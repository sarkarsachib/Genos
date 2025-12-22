package com.example.androidproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    /**
     * Initializes the activity and sets the Jetpack Compose UI to display the Greeting composable.
     *
     * @param savedInstanceState If non-null, a Bundle containing the activity's previously saved state. 
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Greeting("Android")
        }
    }
}

/**
 * Displays a greeting message using the given name.
 *
 * @param name The name to include in the greeting text.
 * @param modifier Modifier to apply to the Text composable.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

/**
 * Renders a design-time preview of Greeting using the name "Android".
 */
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Greeting("Android")
}