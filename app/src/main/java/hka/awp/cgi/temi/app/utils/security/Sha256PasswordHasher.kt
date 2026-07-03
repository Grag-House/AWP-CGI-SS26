package hka.awp.cgi.temi.app.utils.security

import java.security.MessageDigest

/**
 * Implementation of [PasswordHasher] using the SHA-256 algorithm.
 */
class Sha256PasswordHasher : PasswordHasher {
    override fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
