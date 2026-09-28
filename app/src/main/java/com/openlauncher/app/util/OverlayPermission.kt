package com.openlauncher.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

fun openOverlaySettings(context: Context) {
    val intents = listOf(
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    )
    val opened = intents.any { intent ->
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    }
    Toast.makeText(context, if (opened)
        "Select ${context.applicationInfo.loadLabel(context.packageManager)} and enable Allow display over other apps."
        else "This device does not expose overlay settings.", Toast.LENGTH_LONG).show()
}
