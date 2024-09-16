package by.akella.terminalwatch

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

class PermissionsActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions: Map<String, Boolean> ->
            if (permissions.values.all { it }) {
                // Permission granted, proceed with accessing health services
                this.finish()
            } else {
                // Permission denied, handle accordingly (e.g., show an error message)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { requestHealthPermissionsAndReadData() }) {
                        Text("Request Health Permissions")
                    }
                }
            }
        }
    }

    private fun requestHealthPermissionsAndReadData() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can start accessing health services here
                finish()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.BODY_SENSORS) -> {
                // Explain to the user why the permission is needed
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(android.Manifest.permission.BODY_SENSORS,
                    android.Manifest.permission.ACTIVITY_RECOGNITION)
                )
            }
        }
    }
}
