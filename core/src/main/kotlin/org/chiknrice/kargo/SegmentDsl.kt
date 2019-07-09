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

internal data class PropertyContext(val kClass: KClass<*>, val kProperty: KProperty<*>)

open abstract class Segment {

    internal val internalProperties = linkedMapOf<KProperty<*>, SegmentProperty<*>>()

    val properties: Map<KProperty<*>, SegmentProperty<*>>
        get() = internalProperties

    internal companion object {
        val propertyCodecs = mutableMapOf<PropertyContext, PropertyCodec<*>>()
    }

}

class SegmentProperty<T> internal constructor(private val property: KProperty<*>,
                                              private val propertyCodec: PropertyCodec<T>) :
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
        value = propertyCodec.decode(buffer) ?: throw CodecException("Decoded null property [${property.name}]")
    }

    fun encode(buffer: ByteBuffer) {
        internalIndex = buffer.arrayOffset() + buffer.position()
        value?.apply { propertyCodec.encode(value!!, buffer) }
                ?: throw CodecException("Encoding null property [${property.name}]")
    }

}

internal class PropertyCodec<T>(private val codec: Codec<T>, private val context: CodecContext) {

    fun decode(buffer: ByteBuffer) = codec.decode(buffer, context)

    fun encode(value: T, buffer: ByteBuffer) = codec.encode(value, buffer, context)

}

class SegmentPropertyProvider<T> internal constructor(private val codec: Codec<T>, private val context: CodecContext) {

    operator fun provideDelegate(
            thisRef: Segment,
            property: KProperty<*>
    ): ReadWriteProperty<Segment, T?> {

        val propertyCodec = Segment.propertyCodecs.computeIfAbsent(PropertyContext(thisRef::class, property)) {
            PropertyCodec(codec, context)
        } as PropertyCodec<T>

        return SegmentProperty(property, propertyCodec).also {
            thisRef.internalProperties[property] = it
        }
    }

}

class SegmentPropertyProviderBuilder<T> internal constructor(private val codec: Codec<T>,
                                                             private val context: CodecContext) {

    fun <C : Any> override(configClass: KClass<C>, block: ConfigSpec<C>) = context.get(configClass).apply(block)

    internal fun build(): SegmentPropertyProvider<T> {
        return SegmentPropertyProvider(codec, context)
    }

}

fun <T : Any> segment(
        codec: Codec<T>,
        contextTemplate: CodecContextTemplate,
        block: SegmentPropertyProviderBuilder<T>.() -> Unit = {}
): SegmentPropertyProvider<T> =
        SegmentPropertyProviderBuilder(codec, contextTemplate.createNew()).apply(block).build()
