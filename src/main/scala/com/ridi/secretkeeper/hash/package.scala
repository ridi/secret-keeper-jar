package com.ridi.secretkeeper

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

package object hash {
  def bytesToHex(bytes: Array[Byte]): String =
    bytes.map(byte =>
      Integer.toHexString((byte & 0xff) + 0x100).substring(1)
    ).mkString("")

  def hashSHA256(data: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(data.getBytes(StandardCharsets.UTF_8))
    bytesToHex(hash)
  }

  def hmacSHA256(data: String, key: Array[Byte]): Array[Byte] = {
    val algorithm = "HmacSHA256"
    val mac = Mac.getInstance(algorithm)
    mac.init(new SecretKeySpec(key, algorithm))
    mac.doFinal(data.getBytes(StandardCharsets.UTF_8))
  }
}
