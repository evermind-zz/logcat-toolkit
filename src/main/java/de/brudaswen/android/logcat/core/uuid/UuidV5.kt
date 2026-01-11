package de.brudaswen.android.logcat.core.uuid

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

/**
 * Generate namespace name-based [UUID] (version 5).
 *
 * @param namespace The namespace [UUID] to use.
 * @param name The name (String) to use.
 */
internal fun UUID.v5(namespace: UUID, name: String): UUID =
    v5(namespace, name.toByteArray())

/**
 * Generate namespace name-based [UUID] using SHA-1 (UUID version 5).
 *
 * @param namespace The namespace [UUID] to use.
 * @param name The name (ByteArray) to use.
 */
internal fun UUID.v5(namespace: UUID, name: ByteArray): UUID {
    val hash = MessageDigest.getInstance("SHA-1").apply {
        update(namespace.toByteArray())
        update(name)
    }.digest().copyOf(16)

    // Version & Variant anpassen (RFC 4122, v5)
    hash[6] = (hash[6].toInt() and 0x0F or 0x50).toByte() // Version 5
    hash[8] = (hash[8].toInt() and 0x3F or 0x80).toByte() // IETF variant

    return fromByteArray(hash)
}

/**
 * Convert UUID to 16-byte array.
 */
internal fun UUID.toByteArray(): ByteArray =
    ByteBuffer.allocate(16)
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
        .array()

/**
 * Create UUID from 16-byte array.
 */
internal fun UUID.fromByteArray(bytes: ByteArray): UUID {
    require(bytes.size == 16) { "UUID byte array must be exactly 16 bytes" }
    val buffer = ByteBuffer.wrap(bytes)
    return UUID(buffer.long, buffer.long)
}
