package hka.awp.cgi.temi.app.utils.security

/**
 * Interface for hashing passwords to ensure secure storage.
 */
interface PasswordHasher {
    /**
     * Hashes the given [password] string.
     *
     * @param password The raw password to hash.
     * @return The hashed representation of the password.
     */
    fun hashPassword(password: String): String
}
