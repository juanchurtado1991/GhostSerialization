@file:OptIn(InternalGhostApi::class)

package com.ghost.serialization

import com.ghost.serialization.contract.GhostRegistry
import com.ghost.serialization.parser.GhostJsonReader
import com.ghost.serialization.parser.createByteArraySource
import java.util.ServiceLoader

/**
 * JVM-specific implementation for Ghost Serialization discovery.
 * Uses a hybrid approach: Direct load for core + ServiceLoader for modules.
 */
actual fun discoverRegistries(): List<GhostRegistry> {
    val registries = mutableListOf<GhostRegistry>()
    
    // 1. Direct bypass for maximum performance (Zero latency for core)
    try {
        val registryClass = Class.forName("com.ghost.serialization.generated.GhostModuleRegistry_ghost_serialization")
        val instance = registryClass.getDeclaredField("INSTANCE").get(null) as GhostRegistry
        registries.add(instance)
    } catch (e: Exception) {
        // Core registry not found, fallback to ServiceLoader
    }

    // 2. ServiceLoader for modularity (other modules)
    try {
        val loader = ServiceLoader.load(GhostRegistry::class.java)
        for (registry in loader) {
            if (!registries.contains(registry)) {
                registries.add(registry)
            }
        }
    } catch (e: Exception) {
        // ServiceLoader failed
    }
    
    return registries
}

actual fun <T> runSynchronized(lock: Any, block: () -> T): T {
    return synchronized(lock, block)
}

actual fun <T> ghostInternalUseReader(bytes: ByteArray, block: (GhostJsonReader) -> T): T {
    val reader = GhostJsonReader(createByteArraySource(bytes))
    try {
        return block(reader)
    } finally {
        reader.reset(byteArrayOf())
    }
}

actual fun <T> ghostInternalUseSource(source: okio.BufferedSource, block: (GhostJsonReader) -> T): T {
    val reader = GhostJsonReader(createByteArraySource(source.readByteArray()))
    try {
        return block(reader)
    } finally {
        reader.reset(byteArrayOf())
    }
}
