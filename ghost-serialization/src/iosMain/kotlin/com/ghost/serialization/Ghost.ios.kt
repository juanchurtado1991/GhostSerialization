@file:OptIn(InternalGhostApi::class)

package com.ghost.serialization

import com.ghost.serialization.contract.GhostRegistry
import com.ghost.serialization.parser.GhostJsonReader
import com.ghost.serialization.parser.createByteArraySource

/**
 * iOS Implementation of Ghost Registry Discovery.
 * Stub for build completeness.
 */
actual fun discoverRegistries(): List<GhostRegistry> {
    return emptyList()
}

actual fun <T> runSynchronized(lock: Any, block: () -> T): T {
    platform.objc.objc_sync_enter(lock)
    try {
        return block()
    } finally {
        platform.objc.objc_sync_exit(lock)
    }
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
