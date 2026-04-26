@file:OptIn(InternalGhostApi::class)

package com.ghost.serialization.serializers

import com.ghost.serialization.InternalGhostApi
import com.ghost.serialization.contract.GhostSerializer
import com.ghost.serialization.parser.GhostJsonReader
import com.ghost.serialization.parser.ignore
import com.ghost.serialization.parser.nextBoolean
import com.ghost.serialization.parser.nextDouble
import com.ghost.serialization.parser.nextInt
import com.ghost.serialization.parser.nextLong
import com.ghost.serialization.parser.nextString
import com.ghost.serialization.writer.GhostJsonWriter

object StringSerializer : GhostSerializer<String> {
    override val typeName: String = "String"
    override fun serialize(writer: GhostJsonWriter, value: String) {
        writer.value(value).ignore()
    }

    override fun deserialize(reader: GhostJsonReader): String = reader.nextString()
}

object IntSerializer : GhostSerializer<Int> {
    override val typeName: String = "Int"
    override fun serialize(writer: GhostJsonWriter, value: Int) {
        writer.value(value.toLong()).ignore()
    }

    override fun deserialize(reader: GhostJsonReader): Int = reader.nextInt()
}

object LongSerializer : GhostSerializer<Long> {
    override val typeName: String = "Long"
    override fun serialize(writer: GhostJsonWriter, value: Long) {
        writer.value(value).ignore()
    }

    override fun deserialize(reader: GhostJsonReader): Long = reader.nextLong()
}

object BooleanSerializer : GhostSerializer<Boolean> {
    override val typeName: String = "Boolean"
    override fun serialize(writer: GhostJsonWriter, value: Boolean) {
        writer.value(value).ignore()
    }

    override fun deserialize(reader: GhostJsonReader): Boolean = reader.nextBoolean()
}

object DoubleSerializer : GhostSerializer<Double> {
    override val typeName: String = "Double"
    override fun serialize(writer: GhostJsonWriter, value: Double) {
        writer.value(value).ignore()
    }

    override fun deserialize(reader: GhostJsonReader): Double = reader.nextDouble()
}
