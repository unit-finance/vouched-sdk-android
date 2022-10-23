package com.example.unitvouchsampleapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.ArrayList

/**
 * An extension to the UNVouchedService that handles all permission settings and configurations.
 */
fun UNVouchedService.allPermissionsGranted(): Boolean {
    val permissions = getRequiredPermissions()
    for (permission in permissions) {
        if (!isPermissionGranted(
                this.context,
                permission
            )
        ) {
            return false
        }
    }
    return true
}

fun getRequiredPermissions(): Array<String> {
    return arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.CAMERA
    )
}

fun isPermissionGranted(context: Context, permission: String): Boolean {
    if (ContextCompat.checkSelfPermission(context, permission)
        == PackageManager.PERMISSION_GRANTED
    ) {
        println("Permission granted: $permission")
        return true
    }
    println("Permission NOT granted: $permission")
    return false
}

fun UNVouchedService.getRuntimePermissions() {
    val allNeededPermissions: MutableList<String> = ArrayList()
    for (permission in getRequiredPermissions()) {
        if (!isPermissionGranted(
                this.context,
                permission
            )
        ) {
            allNeededPermissions.add(permission)
        }
    }
    if (allNeededPermissions.isNotEmpty()) {
        ActivityCompat.requestPermissions(
            this.context as Activity,
            allNeededPermissions.toTypedArray(),
            PERMISSION_REQUESTS
        )
    }
}