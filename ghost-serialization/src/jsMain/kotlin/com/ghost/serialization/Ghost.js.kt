@file:OptIn(InternalGhostApi::class)

package com.ghost.serialization

import com.ghost.serialization.contract.GhostRegistry
import com.ghost.serialization.parser.GhostJsonReader

import com.ghost.serialization.parser.createByteArraySource

/**
 * JS Discovery.
 */
actual fun discoverRegistries(): List<GhostRegistry> = emptyList()

actual fun <T> runSynchronized(lock: Any, block: () -> T): T {
    return block()
}

actual fun <T> ghostInternalUseReader(
    bytes: ByteArray,
    block: (GhostJsonReader) -> T
): T {
    val reader = GhostJsonReader(createByteArraySource(bytes))
    return block(reader)
}

actual fun <T> ghostInternalUseSource(
    source: okio.BufferedSource,
    block: (GhostJsonReader) -> T
): T {
    val bytes = source.readByteArray()
    val reader = GhostJsonReader(createByteArraySource(bytes))
    return block(reader)
}
