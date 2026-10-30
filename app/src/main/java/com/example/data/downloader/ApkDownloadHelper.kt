package com.example.data.downloader

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class ApkDownloadHelper(private val context: Context) {

    fun downloadAndInstallApk(
        apkUrl: String,
        platformName: String,
        onDownloadStarted: (downloadId: Long) -> Unit,
        onError: (message: String) -> Unit
    ) {
        if (apkUrl.isBlank() || (!apkUrl.startsWith("http://") && !apkUrl.startsWith("https://"))) {
            onError("Invalid APK URL: Must start with http:// or https://")
            return
        }

        try {
            val sanitizedName = platformName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "${sanitizedName}_${System.currentTimeMillis()}.apk"

            val destinationFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )

            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Downloading $platformName APK")
                .setDescription("GyanSarthi Platform Download")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destinationFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            onDownloadStarted(downloadId)

            Toast.makeText(context, "Download started for $platformName APK...", Toast.LENGTH_SHORT).show()

            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == downloadId) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (e: Exception) {
                            // Receiver already unregistered
                        }
                        handleDownloadCompleted(destinationFile, platformName)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

        } catch (e: Exception) {
            onError("Failed to initiate download: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun handleDownloadCompleted(file: File, platformName: String) {
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(context, "Download failed or file empty", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(context, "$platformName downloaded. Preparing installation...", Toast.LENGTH_SHORT).show()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(
                    context,
                    "Please allow installation from GyanSarthi to proceed.",
                    Toast.LENGTH_LONG
                ).show()
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        installApk(file)
    }

    fun installApk(file: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
