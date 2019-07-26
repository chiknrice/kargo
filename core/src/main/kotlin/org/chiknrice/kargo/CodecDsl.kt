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
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.*

internal class ValueCodec<T : Any>(private val encodeSpec: EncodeSpec<T>, private val decodeSpec: DecodeSpec<T>) :
        Codec<T> {
    override fun encode(value: T, buffer: ByteBuffer) = encodeSpec(value, buffer)
    override fun decode(buffer: ByteBuffer) = decodeSpec(buffer)
}

internal class C<T : Any> : DefineCodecDsl<T> {
    override infix fun <C : Any> withConfig(configClass: KClass<C>) = CC<T, C>(configClass)

    override infix fun thatEncodesBy(encodeSpec: EncodeSpec<T>) = CE(encodeSpec)

    class CC<T : Any, C : Any>(private val configClass: KClass<C>) :
            ConfigurableCodecDsl<T, C> {
        override infix fun thatEncodesBy(encodeSpec: ConfigurableEncodeSpec<T, C>) = CCE(configClass, encodeSpec)

        class CCE<T : Any, C : Any>(private val configClass: KClass<C>,
                                    private val encodeSpec: ConfigurableEncodeSpec<T, C>
        ) : AndDecodesByDsl<T, ConfigurableDecodeSpec<T, C>, ConfigurableCodecDefinition<T, C>> {
            override infix fun andDecodesBy(decodeSpec: ConfigurableDecodeSpec<T, C>) =
                    object : ConfigurableCodecDefinition<T, C> {
                        override fun buildCodec(override: OverrideConfigSpec<C>) =
                                configClass.createConfigInstance().apply(override).let { config ->
                                    ValueCodec({ value, buffer -> encodeSpec(value, buffer, config) },
                                            { buffer -> decodeSpec(buffer, config) })
                                }
                    }
        }

    }

    class CE<T : Any>(private val encodeSpec: EncodeSpec<T>) :
            AndDecodesByDsl<T, DecodeSpec<T>, CodecDefinition<T>> {
        override infix fun andDecodesBy(decodeSpec: DecodeSpec<T>) = object : CodecDefinition<T> {
            override fun buildCodec() = ValueCodec(encodeSpec, decodeSpec)
        }
    }

}

internal class F<T : Any> : DefineCodecFilterDsl<T> {
    override infix fun <C : Any> withConfig(configClass: KClass<C>) = FC<T, C>(configClass)

    override infix fun thatEncodesBy(filterEncodeSpec: FilterEncodeSpec<T>) = FE(filterEncodeSpec)

    class FC<T : Any, C : Any>(private val configClass: KClass<C>) :
            ConfigurableCodecFilterDsl<T, C> {
        override infix fun thatEncodesBy(filterEncodeSpec: ConfigurableFilterEncodeSpec<T, C>) =
                FCE(configClass, filterEncodeSpec)

        class FCE<T : Any, C : Any>(private val configClass: KClass<C>,
                                    private val filterEncodeSpec: ConfigurableFilterEncodeSpec<T, C>
        ) : AndDecodesByDsl<T, ConfigurableFilterDecodeSpec<T, C>, ConfigurableFilterDefinition<T, C>> {
            override infix fun andDecodesBy(filterDecodeSpec: ConfigurableFilterDecodeSpec<T, C>) =
                    object : ConfigurableFilterDefinition<T, C> {
                        override fun wrapCodec(chain: Codec<T>, override: OverrideConfigSpec<C>) =
                                configClass.createConfigInstance().apply(override).let { config ->
                                    ValueCodec({ value, buffer ->
                                        filterEncodeSpec(value, buffer, config, chain)
                                    }, { buffer ->
                                        filterDecodeSpec(buffer, config, chain)
                                    })
                                }

                    }
        }
    }

    class FE<T : Any>(private val filterEncodeSpec: FilterEncodeSpec<T>) :
            AndDecodesByDsl<T, FilterDecodeSpec<T>, FilterDefinition<T>> {
        override infix fun andDecodesBy(
                filterDecodeSpec: FilterDecodeSpec<T>) = object : FilterDefinition<T> {
            override fun wrapCodec(chain: Codec<T>) =
                    ValueCodec({ value, buffer -> filterEncodeSpec(value, buffer, chain) },
                            { buffer -> filterDecodeSpec(buffer, chain) })
        }
    }
}

/**
 * PropertyContext defines the coordinates of a particular property (e.g. in which class the property belongs to)
 */
internal data class PropertyContext(val kClass: KClass<*>, val kProperty: KProperty<*>)

internal class S<T : Any> : DefineSegmentPropertyDsl<T> {
    override infix fun using(codecDefinition: CodecDefinition<T>) = SC(codecDefinition)
    override infix fun <C : Any> using(codecDefinition: ConfigurableCodecDefinition<T, C>) =
            SCC(codecDefinition)

    class SC<T : Any>(private val codecDefinition: CodecDefinition<T>) : SegmentPropertyProvider<T>() {
        override fun buildCodec() = codecDefinition.buildCodec()
    }

    class SCC<T : Any, C : Any>(private val codecDefinition: ConfigurableCodecDefinition<T, C>) :
            SegmentPropertyProvider<T>(), WrappedWithOrConfigWithDsl<T, C> {
        private var overrideConfigSpec: OverrideConfigSpec<C> = {}
        override infix fun withConfig(overrideConfigSpec: OverrideConfigSpec<C>) =
                this.also { this.overrideConfigSpec = overrideConfigSpec }

        override fun buildCodec() = codecDefinition.buildCodec(overrideConfigSpec)
    }

    class SF<T : Any>(private val chain: Codec<T>, private val filterDefinition: FilterDefinition<T>) :
            SegmentPropertyProvider<T>() {
        override fun buildCodec() = filterDefinition.wrapCodec(chain)
    }

    class SCF<T : Any, C : Any>(private val codec: Codec<T>,
                                private val filterDefinition: ConfigurableFilterDefinition<T, C>) :
            SegmentPropertyProvider<T>(), WrappedWithOrConfigWithDsl<T, C>, ThenWithOrWithConfigDsl<T, C> {
        private var overrideConfigSpec: OverrideConfigSpec<C> = {}
        override infix fun withConfig(overrideConfigSpec: OverrideConfigSpec<C>) =
                this.also { this.overrideConfigSpec = overrideConfigSpec }

        override fun buildCodec() = filterDefinition.wrapCodec(codec, overrideConfigSpec)
    }

    /**
     * A common class which provides a SegmentProperty in any scenario which is possible to terminate a
     * defineSegmentProperty statement
     */
    abstract class SegmentPropertyProvider<T : Any> : WrappedWithDsl<T>, ThenWithDsl<T> {

        override infix fun wrappedWith(filterDefinition: FilterDefinition<T>) = SF(buildCodec(), filterDefinition)
        override infix fun <C : Any> wrappedWith(filterDefinition: ConfigurableFilterDefinition<T, C>) =
                SCF(buildCodec(), filterDefinition)

        override infix fun thenWith(filterDefinition: FilterDefinition<T>) = SF(buildCodec(), filterDefinition)
        override infix fun <C : Any> thenWith(filterDefinition: ConfigurableFilterDefinition<T, C>) =
                SCF(buildCodec(), filterDefinition)

        abstract fun buildCodec(): Codec<T>

        override operator fun provideDelegate(thisRef: Segment,
                                              property: KProperty<*>): ReadWriteProperty<Segment, T?> {
            // property codecs scope is only one per class-property - while segment property is one per segment instance
            val segmentClass = thisRef::class
            val propertyContext = PropertyContext(segmentClass, property)
            if (!propertyCodecs.containsKey(propertyContext)) {
                synchronized(segmentClass) {
                    if (!propertyCodecs.containsKey(propertyContext)) {
                        propertyCodecs[propertyContext] = buildCodec()
                    }
                }
            }
            return SegmentProperty(propertyContext, propertyCodecs[propertyContext] as Codec<T>).also {
                thisRef.properties.add(it)
            }
        }
    }

    companion object {
        val propertyCodecs = mutableMapOf<PropertyContext, Codec<*>>()
    }

}

internal class SC<T : Segment>(private val segmentClass: KClass<T>) : DefineSegmentCodecDsl<T> {

    override fun thatEncodesBy(encodeSegmentSpec: EncodeSegmentSpec<T>) = SCE(segmentClass, encodeSegmentSpec)

    class SCE<T : Segment>(private val segmentClass: KClass<T>, private val encodeSegmentSpec: EncodeSegmentSpec<T>) :
            AndDecodesByDsl<T, DecodeSegmentSpec<T>, CodecDefinition<T>>, SegmentCodecDefinitionBuilder<T>() {
        override fun andDecodesBy(decodeSegmentSpec: DecodeSegmentSpec<T>) =
                createSegmentCodecDefinition(segmentClass, encodeSegmentSpec, decodeSegmentSpec)
    }

    abstract class SegmentCodecDefinitionBuilder<T : Segment> {
        fun createSegmentCodecDefinition(segmentClass: KClass<T>, encodeSegmentSpec: EncodeSegmentSpec<T>,
                                         decodeSegmentSpec: DecodeSegmentSpec<T>): CodecDefinition<T> {
            validate(segmentClass)
            return C<T>() thatEncodesBy { value, buffer ->
                encodeSegmentSpec(value, SegmentProperties(value.properties), buffer)
            } andDecodesBy { buffer ->
                segmentClass.createSegmentInstance().apply {
                    decodeSegmentSpec(SegmentProperties(this.properties), buffer, this)
                    if (buffer.hasRemaining()) throw CodecException("Buffer still has remaining bytes")
                }
            }
        }

        private fun validate(segmentClass: KClass<T>) {
            val instance = segmentClass.createSegmentInstance()
            segmentClass.memberProperties.filter {
                it.returnType.isSubtypeOf(DelegateProvider::class.createType(
                        listOf(KTypeProjection.invariant(Any::class.starProjectedType))))
            }.map { it.name }.toList().also {
                if (it.isNotEmpty()) throw CodecConfigurationException("Properties are incorrectly assigned: $it")
            }
            if (instance.properties.isEmpty()) {
                throw CodecConfigurationException(
                        "Segment class [${segmentClass.simpleName}] is required to have a segment property")
            }
        }
    }

}

internal fun <T : Any> KClass<T>.createConfigInstance() = try {
    this.createInstance()
} catch (e: Exception) {
    throw CodecConfigurationException("Failed to create configuration class instance: ${this.simpleName}", e)
}

internal fun <T : Segment> KClass<T>.createSegmentInstance() = try {
    this.createInstance()
} catch (e: Exception) {
    throw CodecConfigurationException("Failed to create segment class instance: ${this.simpleName}", e)
}

