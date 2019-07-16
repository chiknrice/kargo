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
import kotlin.reflect.KProperty

open abstract class Segment {

    internal val properties = mutableListOf<SegmentProperty<*>>()

}

class SegmentProperty<T : Any> internal constructor(internal val context: PropertyContext,
                                                    private val codec: Codec<T>) :
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
        value = try {
            codec.decode(buffer)
        } catch (e: Exception) {
            throw CodecException("Error decoding [${context.kClass.simpleName}.${context.kProperty.name}]", e)
        }
    }

    fun encode(buffer: ByteBuffer) {
        internalIndex = buffer.arrayOffset() + buffer.position()
        value?.apply {
            try {
                codec.encode(value!!, buffer)
            } catch (e: Exception) {
                throw CodecException("Error encoding [${context.kClass.simpleName}.${context.kProperty.name}]", e)
            }
        } ?: throw CodecException("Encoding null property [${context.kClass.simpleName}.${context.kProperty.name}]")
    }

}

class SegmentProperties(private val inner: List<SegmentProperty<*>>) : List<SegmentProperty<*>> by inner