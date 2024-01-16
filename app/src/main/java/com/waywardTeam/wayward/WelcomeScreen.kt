package com.waywardTeam.wayward

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.wayward.R

/**
 * Welcome screen for showing users necessary information about permissions, usage and functions
 */
class WelcomeScreen : AppCompatActivity() {
    // Function called on activity starts up
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome_screen)

        // Checking if the application got permissions that it needs
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // It got all of its permissions, so it is redirecting to the next activity
            next()
        }

        // Button for granting permissions
        val checkPermissionButton: Button = findViewById(R.id.grantPermission)
        checkPermissionButton.setOnClickListener {
            request()
        }
    }

    /**
     * Handling permission request
     * Will loop over each permission that it needs for program operating properly
     */
    private fun request() {
        // List for storing permissions
        val perm = mutableListOf<String>()

        // Location
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            perm.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Notification
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perm.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Requesting permissions
        if (perm.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, perm.toTypedArray(), 1
            )
        }
    }

    /**
     * Redirecting to next activity
     */
    private fun next() {
        val intent = Intent(this, RouteSearchActivity::class.java)
        startActivity(intent)
        finish()
    }

    // This function is called when it got back permission results
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        // Calling original code from on onRequestPermissionsResult
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        next()
    }
}