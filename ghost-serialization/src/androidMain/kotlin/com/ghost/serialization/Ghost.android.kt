@file:OptIn(InternalGhostApi::class)

package com.ghost.serialization

import com.ghost.serialization.contract.GhostRegistry
import com.ghost.serialization.parser.GhostJsonReader
import com.ghost.serialization.parser.createByteArraySource
import java.util.ServiceLoader

/**
 * Android-specific implementation for Ghost Serialization discovery.
 * Hybrid approach for maximum startup performance.
 */
actual fun discoverRegistries(): List<GhostRegistry> {
    val registries = mutableListOf<GhostRegistry>()
    
    // 1. Direct bypass (Zero latency for core)
    try {
        val registryClass = Class.forName("com.ghost.serialization.generated.GhostModuleRegistry_ghost_serialization")
        val instance = registryClass.getDeclaredField("INSTANCE").get(null) as GhostRegistry
        registries.add(instance)
    } catch (e: Exception) {
    }

    // 2. ServiceLoader fallback
    try {
        val loader = ServiceLoader.load(GhostRegistry::class.java)
        for (registry in loader) {
            if (!registries.contains(registry)) {
                registries.add(registry)
            }
        }
    } catch (e: Exception) {
    }
    
    return registries
}

actual fun <T> runSynchronized(lock: Any, block: () -> T): T {
    return synchronized(lock, block)
}

actual fun <T> ghostInternalUseReader(bytes: ByteArray, block: (GhostJsonReader) -> T): T {
    val reader = GhostJsonReader(createByteArraySource(bytes))
    return block(reader)
}

actual fun <T> ghostInternalUseSource(source: okio.BufferedSource, block: (GhostJsonReader) -> T): T {
    val bytes = source.readByteArray()
    val reader = GhostJsonReader(createByteArraySource(bytes))
    return block(reader)
}
