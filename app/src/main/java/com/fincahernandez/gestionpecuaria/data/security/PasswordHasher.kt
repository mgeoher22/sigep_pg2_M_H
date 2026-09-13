package com.fincahernandez.gestionpecuaria.data.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class ProtectedPassword(
    val hash: String,
    val salt: String,
    val algorithm: String,
    val iterations: Int
)

/** Protege contraseñas mediante PBKDF2, sal aleatoria y comparación constante. */
object PasswordHasher {
    private const val PRIMARY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val FALLBACK_ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    fun protect(password: String): ProtectedPassword {
        require(password.length >= 8) { "La contraseña debe tener al menos 8 caracteres." }
        return protectValidatedCloudPassword(password)
    }

    /** Supabase already validated this password; accept its configured length policy. */
    fun protectValidatedCloudPassword(password: String): ProtectedPassword {
        require(password.isNotEmpty()) { "La contraseña no puede estar vacía." }
        val salt = ByteArray(SALT_LENGTH_BYTES).also(SecureRandom()::nextBytes)
        val algorithm = availableAlgorithm()
        val hash = derive(password, salt, algorithm, ITERATIONS)
        return ProtectedPassword(
            hash = hash.toBase64(),
            salt = salt.toBase64(),
            algorithm = algorithm,
            iterations = ITERATIONS
        )
    }

    fun verify(
        password: String,
        expectedHash: String,
        salt: String,
        algorithm: String,
        iterations: Int
    ): Boolean = runCatching {
        val actual = derive(
            password = password,
            salt = Base64.decode(salt, Base64.NO_WRAP),
            algorithm = algorithm,
            iterations = iterations
        )
        val expected = Base64.decode(expectedHash, Base64.NO_WRAP)
        MessageDigest.isEqual(actual, expected)
    }.getOrDefault(false)

    private fun derive(
        password: String,
        salt: ByteArray,
        algorithm: String,
        iterations: Int
    ): ByteArray {
        val passwordChars = password.toCharArray()
        val specification = PBEKeySpec(passwordChars, salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(algorithm).generateSecret(specification).encoded
        } finally {
            specification.clearPassword()
            passwordChars.fill('\u0000')
        }
    }

    private fun availableAlgorithm(): String = runCatching {
        SecretKeyFactory.getInstance(PRIMARY_ALGORITHM)
        PRIMARY_ALGORITHM
    }.getOrElse { FALLBACK_ALGORITHM }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
}
