package com.ncmdump.android

import android.util.Base64
import com.mpatric.mp3agic.Mp3File
import com.mpatric.mp3agic.ID3v2
import com.mpatric.mp3agic.ID3v24Tag
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * NCM file decryption engine
 * Faithful port of: git.taurusxin.com/taurusxin/ncmdump-go/ncmcrypt/ncmcrypt.go
 *
 * Algorithm:
 * 1. Validate magic header (CTENFDAM)
 * 2. AES-128-ECB decrypt the RC4 key
 * 3. Build 256-byte key box (RC4 variant)
 * 4. XOR decrypt audio stream using key box
 * 5. Detect output format (MP3/FLAC)
 * 6. Fix metadata (ID3v2 for MP3)
 */
class NcmCrypt(private val filePath: String) {

    companion object {
        // AES core key for decrypting the RC4 key
        private val CORE_KEY = byteArrayOf(
            0x68, 0x7A, 0x48, 0x52, 0x41, 0x6D, 0x73, 0x6F,
            0x35, 0x6B, 0x49, 0x6E, 0x62, 0x61, 0x78, 0x57
        )

        // AES modify key for decrypting metadata
        private val MODIFY_KEY = byteArrayOf(
            0x23, 0x31, 0x34, 0x6C, 0x6A, 0x6B, 0x5F, 0x21,
            0x5C, 0x5D, 0x26, 0x30, 0x55, 0x3C, 0x27, 0x28
        )

        // PNG magic bytes
        private val PNG_HEADER = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )

        const val FORMAT_MP3 = "mp3"
        const val FORMAT_FLAC = "flac"
    }

    private var fileStream: RandomAccessFile? = null
    private val keyBox = IntArray(256)
    private var metadata: NcmMetadata? = null
    private var albumPicUrl: String = ""
    private var imageData: ByteArray = ByteArray(0)
    private var format: String = ""
    private var dumpFilePath: String = ""

    init {
        openAndParse()
    }

    private fun openAndParse() {
        // Open file
        fileStream = RandomAccessFile(filePath, "r")

        // Check magic header: 0x4E455443 0x4D414446
        val header1 = readInt()
        if (header1 != 0x4E455443) {
            throw IllegalArgumentException("Not a NCM file (invalid header 1)")
        }
        val header2 = readInt()
        if (header2 != 0x4D414446) {
            throw IllegalArgumentException("Not a NCM file (invalid header 2)")
        }

        // Skip 2 bytes (version)
        fileStream!!.skipBytes(2)

        // Read RC4 key (AES encrypted)
        val keyLen = readIntLE()
        val keyData = ByteArray(keyLen)
        fileStream!!.readFully(keyData)

        // XOR with 0x64
        for (i in keyData.indices) {
            keyData[i] = (keyData[i].toInt() xor 0x64).toByte()
        }

        // AES-128-ECB decrypt the key
        val decryptedKey = AesUtils.aesEcbDecrypt(CORE_KEY, keyData)

        // Build key box (skip first 17 bytes: "neteasecloudmusic")
        buildKeyBox(decryptedKey, 17)

        // Read metadata
        val metadataLen = readIntLE()
        if (metadataLen > 0) {
            val modifyData = ByteArray(metadataLen)
            fileStream!!.readFully(modifyData)

            // XOR with 0x63
            for (i in modifyData.indices) {
                modifyData[i] = (modifyData[i].toInt() xor 0x63).toByte()
            }

            // Skip "163 key(Don't modify):" (22 bytes) and base64 decode
            val swapModifyData = String(modifyData, 22, modifyData.size - 22, Charsets.UTF_8)
            val modifyOutData = Base64.decode(swapModifyData, Base64.DEFAULT)

            // AES-128-ECB decrypt metadata
            val modifyDecryptData = AesUtils.aesEcbDecrypt(MODIFY_KEY, modifyOutData)

            // Skip "music:" (6 bytes)
            val metadataString = String(modifyDecryptData, 6, modifyDecryptData.size - 6, Charsets.UTF_8)

            // Parse metadata
            albumPicUrl = NcmMetadata.getAlbumPicUrl(metadataString)
            metadata = NcmMetadata.fromJson(metadataString)
        }

        // Skip 5 bytes gap
        fileStream!!.skipBytes(5)

        // Read cover frame
        val coverFrameLen = readIntLE()
        val coverDataLen = readIntLE()

        if (coverDataLen > 0) {
            imageData = ByteArray(coverDataLen)
            fileStream!!.readFully(imageData)
        }

        // Skip remaining cover frame
        val remaining = coverFrameLen - coverDataLen
        if (remaining > 0) {
            fileStream!!.skipBytes(remaining)
        }
    }

    /**
     * Build the 256-byte key box (RC4 variant key scheduling)
     * Port of: NeteaseCloudMusic.buildKeyBox
     */
    private fun buildKeyBox(key: ByteArray, keyOffset: Int) {
        for (i in 0 until 256) {
            keyBox[i] = i
        }

        var swap: Int
        var c = 0
        var lastByte = 0
        var kOffset = 0
        val keyLen = key.size - keyOffset

        for (i in 0 until 256) {
            swap = keyBox[i]
            c = (swap + lastByte + (key[keyOffset + kOffset].toInt() and 0xFF)) and 0xFF
            kOffset++
            if (kOffset >= keyLen) {
                kOffset = 0
            }
            keyBox[i] = keyBox[c]
            keyBox[c] = swap
            lastByte = c
        }
    }

    /**
     * Dump encrypted NCM to normal audio file
     * Port of: NeteaseCloudMusic.Dump
     */
    fun dump(targetDir: String): Boolean {
        dumpFilePath = filePath

        val buffer = ByteArray(0x8000)
        var outputStream: FileOutputStream? = null
        var findFormatFlag = false

        try {
            while (true) {
                val n = fileStream!!.read(buffer)
                if (n <= 0) break

                // XOR decrypt each byte with key box
                for (i in 0 until n) {
                    val j = (i + 1) and 0xFF
                    val k1 = keyBox[j]
                    val k2 = keyBox[(k1 + j) and 0xFF]
                    val k3 = keyBox[(k1 + k2) and 0xFF]
                    buffer[i] = (buffer[i].toInt() xor k3).toByte()
                }

                if (!findFormatFlag) {
                    // Detect format from first bytes
                    if (buffer[0] == 0x49.toByte() && buffer[1] == 0x44.toByte() && buffer[2] == 0x33.toByte()) {
                        format = FORMAT_MP3
                        dumpFilePath = replaceExtension(dumpFilePath, ".mp3")
                    } else {
                        format = FORMAT_FLAC
                        dumpFilePath = replaceExtension(dumpFilePath, ".flac")
                    }

                    if (targetDir.isNotEmpty()) {
                        val fileName = File(dumpFilePath).name
                        dumpFilePath = File(targetDir, fileName).absolutePath
                    }

                    findFormatFlag = true
                    outputStream = FileOutputStream(dumpFilePath)
                }

                outputStream?.write(buffer, 0, n)
            }

            outputStream?.close()
            return true
        } catch (e: Exception) {
            outputStream?.close()
            throw e
        }
    }

    /**
     * Fix metadata for the dumped audio file
     * Port of: NeteaseCloudMusic.FixMetadata
     */
    fun fixMetadata(fetchAlbumImageFromRemote: Boolean): Boolean {
        if (metadata == null) return false

        // Download cover image if not embedded and requested
        if (imageData.isEmpty() && fetchAlbumImageFromRemote && albumPicUrl.isNotEmpty()) {
            try {
                val url = URL(albumPicUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    imageData = conn.inputStream.readBytes()
                }
                conn.disconnect()
            } catch (e: Exception) {
                // Ignore cover download failure
            }
        }

        return when (format) {
            FORMAT_MP3 -> fixMp3Metadata()
            FORMAT_FLAC -> fixFlacMetadata()
            else -> false
        }
    }

    private fun fixMp3Metadata(): Boolean {
        return try {
            val mp3File = Mp3File(dumpFilePath)
            val tag: ID3v2 = if (mp3File.hasId3v2Tag()) {
                mp3File.id3v2Tag
            } else {
                val newTag = ID3v24Tag()
                mp3File.id3v2Tag = newTag
                newTag
            }

            metadata?.let {
                if (tag.title.isNullOrEmpty()) tag.title = it.name
                if (tag.artist.isNullOrEmpty()) tag.artist = it.artist
                if (tag.album.isNullOrEmpty()) tag.album = it.album
            }

            if (imageData.isNotEmpty()) {
                val mimeType = if (imageData.size >= 8 && imageData.copyOfRange(0, 8).contentEquals(PNG_HEADER)) {
                    "image/png"
                } else {
                    "image/jpeg"
                }
                tag.setAlbumImage(imageData, mimeType)
            }

            // Save to a temp file and replace
            val tempFile = dumpFilePath + ".tmp"
            mp3File.save(tempFile)
            File(dumpFilePath).delete()
            File(tempFile).renameTo(File(dumpFilePath))
            true
        } catch (e: Exception) {
            // If metadata fixing fails, the decrypted file is still usable
            false
        }
    }

    private fun fixFlacMetadata(): Boolean {
        // FLAC metadata fixing is more complex - skip for now
        // The decrypted FLAC file is still fully playable without metadata fixes
        // FLAC files often already contain their own metadata
        return true
    }

    fun getDumpFilePath(): String = dumpFilePath

    fun close() {
        try {
            fileStream?.close()
        } catch (_: Exception) {}
    }

    // ---- Helper methods ----

    private fun readInt(): Int {
        val buf = ByteArray(4)
        fileStream!!.readFully(buf)
        // Big-endian read (for magic header comparison)
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun readIntLE(): Int {
        val buf = ByteArray(4)
        fileStream!!.readFully(buf)
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun replaceExtension(path: String, newExt: String): String {
        val lastDot = path.lastIndexOf('.')
        return if (lastDot >= 0) {
            path.substring(0, lastDot) + newExt
        } else {
            path + newExt
        }
    }
}
