package com.ghost.serialization.generated
import com.ghost.serialization.Ghost

object GhostAutoRegistry {
    fun registerAll() {
        try {
            Ghost.addRegistry(GhostModuleRegistry_ghost_serialization.INSTANCE)
        } catch (e: Throwable) {}
    }
}
