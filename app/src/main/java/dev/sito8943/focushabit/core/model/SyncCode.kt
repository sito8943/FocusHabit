package dev.sito8943.focushabit.core.model

import java.security.SecureRandom

/**
 * The 4-character recovery code that scopes every habit on the server.
 * Same rules as the web app (`features/sync/providers/SyncCode/utils.ts`).
 */
object SyncCode {

    const val LENGTH = 4

    /** No I, O, 0 or 1: they are easy to confuse when copying the code by hand. */
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    private val VALID = Regex("^[A-Z0-9]{$LENGTH}$")
    private val NOT_ALLOWED = Regex("[^A-Z0-9]")

    private val random = SecureRandom()

    fun generate(): String = buildString {
        repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    /** Uppercase, strip anything that is not A-Z/0-9 and cut to [LENGTH]. */
    fun normalize(value: String): String =
        value.uppercase().replace(NOT_ALLOWED, "").take(LENGTH)

    /** Accepts any A-Z/0-9 (like the web), even characters [generate] never produces. */
    fun isValid(value: String): Boolean = VALID.matches(value)
}
