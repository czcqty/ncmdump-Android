package com.ncmdump.android

import org.json.JSONObject
import org.json.JSONArray

/**
 * NCM file metadata
 * Port of: git.taurusxin.com/taurusxin/ncmdump-go/ncmcrypt/metadata.go
 */
data class NcmMetadata(
    val album: String = "",
    val artist: String = "",
    val format: String = "",
    val name: String = "",
    val duration: Long = 0,
    val bitrate: Long = 0
) {
    companion object {
        /**
         * Parse metadata from JSON string
         * Port of: NewNeteaseCloudMusicMetadata
         */
        fun fromJson(meta: String): NcmMetadata? {
            if (meta.isEmpty()) return null

            return try {
                val json = JSONObject(meta)
                val name = json.optString("musicName", "")
                val album = json.optString("album", "")

                // Parse artist array: [[name, id], ...]
                val artistBuilder = StringBuilder()
                val artistArray = json.optJSONArray("artist")
                if (artistArray != null) {
                    for (i in 0 until artistArray.length()) {
                        if (i > 0) artistBuilder.append(" / ")
                        val artistEntry = artistArray.optJSONArray(i)
                        if (artistEntry != null && artistEntry.length() > 0) {
                            artistBuilder.append(artistEntry.optString(0, ""))
                        }
                    }
                }

                val bitrate = json.optLong("bitrate", 0)
                val duration = json.optLong("duration", 0)
                val format = json.optString("format", "")

                NcmMetadata(
                    album = album,
                    artist = artistBuilder.toString(),
                    format = format,
                    name = name,
                    duration = duration,
                    bitrate = bitrate
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Extract album picture URL from metadata JSON
         * Port of: GetAlbumPicUrl
         */
        fun getAlbumPicUrl(meta: String): String {
            return try {
                val json = JSONObject(meta)
                json.optString("albumPic", "")
            } catch (e: Exception) {
                ""
            }
        }
    }
}
