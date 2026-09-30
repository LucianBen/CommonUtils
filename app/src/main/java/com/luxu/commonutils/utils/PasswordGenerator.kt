package com.luxu.commonutils.utils

import java.security.SecureRandom

object PasswordGenerator {
    internal const val ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!"
    private val random = SecureRandom()

    fun generate(): String = CharArray(16) { ALPHABET[random.nextInt(ALPHABET.length)] }.concatToString()
}
