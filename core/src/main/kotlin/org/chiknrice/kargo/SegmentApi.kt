/*
 * Copyright (c) 2019 Ian Bondoc
 *
 * This file is part of project "kargo"
 *
 * Project kargo is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or(at your option)
 * any later version.
 *
 * Project kargo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 */

package org.chiknrice.kargo

import java.nio.ByteBuffer
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

open abstract class Segment {

    internal val internalProperties = linkedMapOf<KProperty<*>, SegmentProperty<*>>()

    val properties: Map<KProperty<*>, SegmentProperty<*>>
        get() = internalProperties

}

class SegmentProperty<T : Any> internal constructor(private val property: KProperty<*>,
                                                    private val propertyCodec: Codec<T>) :
        ReadWriteProperty<Segment, T?> {

    private var value: T? = null
    private var internalIndex: Int? = null

    override fun getValue(thisRef: Segment, property: KProperty<*>) = value

    override fun setValue(thisRef: Segment, property: KProperty<*>, value: T?) {
        this.value = value
    }

    val index: Int?
        get() = if (value != null) internalIndex else null

    fun decode(buffer: ByteBuffer) {
        internalIndex = buffer.arrayOffset() + buffer.position()
        value = propertyCodec.decode(buffer)
    }

    fun encode(buffer: ByteBuffer) {
        internalIndex = buffer.arrayOffset() + buffer.position()
        value?.apply { propertyCodec.encode(value!!, buffer) }
                ?: throw CodecException("Encoding null property [${property.name}]")
    }

}

internal data class PropertyContext(val kClass: KClass<*>, val kProperty: KProperty<*>)

class SegmentPropertyProvider<T : Any> internal constructor(private val buildCodec: CodecFactory<T>) {

    operator fun provideDelegate(
            thisRef: Segment,
            property: KProperty<*>
    ): ReadWriteProperty<Segment, T?> {

        val codec = propertyCodecs.computeIfAbsent(PropertyContext(thisRef::class, property)) {
            buildCodec()
        } as Codec<T>

        return SegmentProperty(property, codec).also {
            thisRef.internalProperties[property] = it
        }
    }

    internal companion object {
        val propertyCodecs = mutableMapOf<PropertyContext, Codec<*>>()
    }

}
