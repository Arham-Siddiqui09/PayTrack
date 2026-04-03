package com.paytrack.payment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.paytrack.data.UpiAppInfo

object UpiAppResolver {

    fun resolve(context: Context): List<UpiAppInfo> {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("upi://pay?pa=test@upi&pn=PayTrack")
        )

        val packageManager = context.packageManager
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                android.content.pm.PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        return resolveInfos
            .map { info ->
                UpiAppInfo(
                    label = info.loadLabel(packageManager)?.toString().orEmpty().ifBlank { info.activityInfo.packageName },
                    packageName = info.activityInfo.packageName
                )
            }
            .distinctBy(UpiAppInfo::packageName)
            .sortedBy(UpiAppInfo::label)
    }

    fun launchIntent(context: Context, packageName: String): Intent? {
        return context.packageManager.getLaunchIntentForPackage(packageName)
    }
}
