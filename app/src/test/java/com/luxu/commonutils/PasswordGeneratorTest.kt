package com.luxu.commonutils

import com.luxu.commonutils.utils.PasswordGenerator
import org.junit.Assert.*
import org.junit.Test

class PasswordGeneratorTest {
    @Test fun alphabetContainsEachSupportedCharacterExactlyOnce() {
        val alphabet = PasswordGenerator.ALPHABET
        assertEquals(alphabet.length, alphabet.toSet().size)
        assertEquals(('a'..'z').toSet() + ('A'..'Z').toSet() + ('0'..'9').toSet() + '!', alphabet.toSet())
    }
    @Test fun generatedPasswordsHaveExpectedLengthAndAllowedCharacters() {
        repeat(100) {
            val password = PasswordGenerator.generate()
            assertEquals(16, password.length)
            assertTrue(password.all { it in PasswordGenerator.ALPHABET })
        }
    }
}
