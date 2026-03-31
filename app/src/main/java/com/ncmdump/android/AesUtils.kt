package com.ncmdump.android

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * AES-128-ECB decryption utility
 * Port of: git.taurusxin.com/taurusxin/ncmdump-go/utils.AesEcbDecrypt
 */
object AesUtils {

    /**
     * Decrypt data using AES-128-ECB with PKCS5 padding removal
     */
    fun aesEcbDecrypt(key: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }
}
