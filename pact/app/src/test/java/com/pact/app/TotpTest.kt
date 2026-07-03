package com.pact.app

import com.pact.app.core.Totp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpTest {

    // RFC 6238 test secret: ASCII "12345678901234567890"
    private val rfcSecret = Totp.base32Encode("12345678901234567890".toByteArray())

    /**
     * RFC 6238 Appendix B vectors (SHA1). The RFC lists 8-digit codes; the
     * 6-digit equivalents are the last 6 digits of each.
     */
    @Test
    fun rfc6238Vectors() {
        val vectors = mapOf(
            59L to "94287082",
            1111111109L to "07081804",
            1111111111L to "14050471",
            1234567890L to "89005924",
            2000000000L to "69279037",
            20000000000L to "65353130",
        )
        for ((epochSeconds, eightDigit) in vectors) {
            val step = epochSeconds / 30
            assertEquals(eightDigit.takeLast(6), Totp.codeAt(rfcSecret, step))
        }
    }

    @Test
    fun base32RoundTrip() {
        val data = ByteArray(20) { (it * 7).toByte() }
        assertEquals(data.toList(), Totp.base32Decode(Totp.base32Encode(data)).toList())
    }

    @Test
    fun base32DecodeToleratesFormatting() {
        val secret = Totp.generateSecret()
        val spaced = Totp.prettySecret(secret).lowercase()
        assertEquals(
            Totp.base32Decode(secret).toList(),
            Totp.base32Decode(spaced).toList(),
        )
    }

    @Test
    fun verifyAcceptsCurrentAndAdjacentSteps() {
        val secret = Totp.generateSecret()
        val now = 1_700_000_000_000L
        val step = Totp.stepAt(now)
        for (delta in -2..2) {
            val code = Totp.codeAt(secret, step + delta)
            assertNotNull("delta=$delta should verify", Totp.verify(secret, code, now))
        }
        assertNull(Totp.verify(secret, Totp.codeAt(secret, step + 5), now))
        assertNull(Totp.verify(secret, Totp.codeAt(secret, step - 5), now))
    }

    @Test
    fun verifyRejectsGarbage() {
        val secret = Totp.generateSecret()
        assertNull(Totp.verify(secret, "", 0))
        assertNull(Totp.verify(secret, "12345", 0))
        assertNull(Totp.verify(secret, "abcdef", 0))
        assertNull(Totp.verify(secret, "1234567", 0))
    }

    @Test
    fun secretsAre32CharsBase32() {
        val secret = Totp.generateSecret()
        assertEquals(32, secret.length)
        assertTrue(secret.all { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567" })
    }

    @Test
    fun extractSecretHandlesOtpAuthUrisAndRawKeys() {
        val secret = Totp.generateSecret()
        // scanning the pairing QR (otpauth URI)
        assertEquals(secret, Totp.extractSecret(Totp.otpAuthUri(secret, "Sam")))
        // typing the pretty-printed key by hand, lowercase with spaces
        assertEquals(secret, Totp.extractSecret(Totp.prettySecret(secret).lowercase()))
        // junk is rejected
        assertNull(Totp.extractSecret("otpauth://totp/Pact:Sam?issuer=Pact"))
        assertNull(Totp.extractSecret("not a key at all!"))
        assertNull(Totp.extractSecret("SHORT"))
    }

    @Test
    fun otpAuthUriIsWellFormed() {
        val uri = Totp.otpAuthUri("ABC234", "Sam Smith")
        assertTrue(uri.startsWith("otpauth://totp/Pact:Sam%20Smith?secret=ABC234&issuer=Pact"))
        assertTrue(uri.contains("digits=6"))
        assertTrue(uri.contains("period=30"))
        assertTrue(uri.contains("algorithm=SHA1"))
    }
}
