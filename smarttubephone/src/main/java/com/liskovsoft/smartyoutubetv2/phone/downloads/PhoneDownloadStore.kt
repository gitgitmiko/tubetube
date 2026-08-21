package com.liskovsoft.smartyoutubetv2.phone.downloads

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.phone.R
import org.json.JSONArray
import org.json.JSONObject

data class PhoneDownloadItem(
    val videoId: String,
    val title: String,
    val quality: String,
    val downloadId: Long
)

object PhoneDownloadStore {
    private const val PREFS = "phone_downloads"
    private const val KEY = "items"

    fun enqueue(context: Context, video: Video?, formatInfo: MediaItemFormatInfo?): Boolean {
        val title = video?.title ?: formatInfo?.title ?: return false
        val videoId = video?.videoId ?: formatInfo?.videoId ?: "unknown"
            val progressive = formatInfo?.urlFormats
            ?.filter { !it.url.isNullOrBlank() && (it.mimeType?.contains("mp4") == true || it.formatType == com.liskovsoft.mediaserviceinterfaces.data.MediaFormat.FORMAT_TYPE_REGULAR) }
            ?.maxByOrNull { it.height }
            ?: formatInfo?.urlFormats?.firstOrNull { !it.url.isNullOrBlank() }
        val url = progressive?.url
        if (url.isNullOrBlank()) {
            Toast.makeText(context, R.string.download_unavailable, Toast.LENGTH_LONG).show()
            return false
        }
        val quality = progressive.qualityLabel ?: progressive.quality ?: "video"
        val safeName = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80)
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(title)
            setDescription(quality)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "TubeTube/$safeName.mp4")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = manager.enqueue(request)
        save(context, PhoneDownloadItem(videoId, title, quality, id))
        Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show()
        return true
    }

    fun list(context: Context): List<PhoneDownloadItem> {
        val raw = prefs(context).getString(KEY, "[]") ?: "[]"
        val array = JSONArray(raw)
        val items = mutableListOf<PhoneDownloadItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            items.add(
                PhoneDownloadItem(
                    obj.optString("videoId"),
                    obj.optString("title"),
                    obj.optString("quality"),
                    obj.optLong("downloadId")
                )
            )
        }
        return items.asReversed()
    }

    private fun save(context: Context, item: PhoneDownloadItem) {
        val items = list(context).toMutableList()
        items.add(0, item)
        val array = JSONArray()
        items.take(50).forEach {
            array.put(
                JSONObject()
                    .put("videoId", it.videoId)
                    .put("title", it.title)
                    .put("quality", it.quality)
                    .put("downloadId", it.downloadId)
            )
        }
        prefs(context).edit().putString(KEY, array.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
