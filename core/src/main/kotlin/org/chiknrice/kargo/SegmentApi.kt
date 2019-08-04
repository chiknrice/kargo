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

/**
 * PropertyContext defines the coordinates of a particular property (e.g. in which class the property belongs to)
 */
internal data class PropertyContext(val kClass: KClass<*>, val kProperty: KProperty<*>)

abstract class Segment {

    internal val properties = mutableListOf<SegmentProperty<*>>()

    fun <T : Any> codec(codecDefinition: CodecDefinition<T>) = object : DelegateProvider<T> {
        override operator fun provideDelegate(thisRef: Segment,
                                              property: KProperty<*>): ReadWriteProperty<Segment, T?> {
            // property codecs scope is only one per class-property - while segment property is one per segment instance
            val segmentClass = thisRef::class
            val propertyContext = PropertyContext(segmentClass, property)
            if (!propertyCodecs.containsKey(propertyContext)) {
                synchronized(segmentClass) {
                    if (!propertyCodecs.containsKey(propertyContext)) {
                        propertyCodecs[propertyContext] = codecDefinition.buildCodec()
                    }
                }
            }
            return SegmentProperty(propertyContext, propertyCodecs[propertyContext] as Codec<T>).also {
                thisRef.properties.add(it)
            }
        }
    }

    internal companion object {
        val propertyCodecs = mutableMapOf<PropertyContext, Codec<*>>()
    }
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