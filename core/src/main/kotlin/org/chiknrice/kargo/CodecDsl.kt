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

internal class ValueCodec<T : Any>(private val encodeBlock: EncodeBlock<T>, private val decodeBlock: DecodeBlock<T>) :
        Codec<T> {
    override fun encode(value: T, buffer: ByteBuffer) = encodeBlock(value, buffer)
    override fun decode(buffer: ByteBuffer) = decodeBlock(buffer)
}

internal class C<T : Any> : DefineCodecDsl<T> {
    override infix fun <C : Any> withConfig(configClass: KClass<C>) = CC<T, C>(configClass)

    override infix fun thatEncodesBy(encodeBlock: EncodeBlock<T>) = CE(encodeBlock)

    class CC<T : Any, C : Any>(private val configClass: KClass<C>) :
            ConfigurableCodecDsl<T, C> {
        override infix fun thatEncodesBy(encodeBlock: EncodeWithConfigBlock<T, C>) = CCE(configClass, encodeBlock)

        class CCE<T : Any, C : Any>(private val configClass: KClass<C>,
                                    private val encodeBlock: EncodeWithConfigBlock<T, C>
        ) : AndDecodesByDsl<T, DecodeWithConfigBlock<T, C>, BuildConfigurableCodecBlock<T, C>> {
            override infix fun andDecodesBy(decodeBlock: DecodeWithConfigBlock<T, C>):
                    BuildConfigurableCodecBlock<T, C> = { override ->
                configClass.createConfigInstance().apply(override).let { config ->
                    ValueCodec({ value, buffer -> encodeBlock(value, buffer, config) },
                            { buffer -> decodeBlock(buffer, config) })
                }
            }
        }

    }

    class CE<T : Any>(private val encodeBlock: EncodeBlock<T>) :
            AndDecodesByDsl<T, DecodeBlock<T>, BuildCodecBlock<T>> {
        override infix fun andDecodesBy(decodeBlock: DecodeBlock<T>): BuildCodecBlock<T> = {
            ValueCodec(encodeBlock, decodeBlock)
        }
    }

}

internal class F<T : Any> : DefineCodecFilterDsl<T> {
    override infix fun <C : Any> withConfig(configClass: KClass<C>) = FC<T, C>(configClass)

    override infix fun thatEncodesBy(filterEncodeBlock: FilterEncodeBlock<T>) = FE(filterEncodeBlock)

    class FC<T : Any, C : Any>(private val configClass: KClass<C>) :
            ConfigurableCodecFilterDsl<T, C> {
        override infix fun thatEncodesBy(filterEncodeBlock: FilterEncodeWithConfigBlock<T, C>) =
                FCE(configClass, filterEncodeBlock)

        class FCE<T : Any, C : Any>(private val configClass: KClass<C>,
                                    private val filterEncodeBlock: FilterEncodeWithConfigBlock<T, C>
        ) : AndDecodesByDsl<T, FilterDecodeWithConfigBlock<T, C>, WrapCodecWithConfigurableFilterBlock<T, C>> {
            override infix fun andDecodesBy(filterDecodeBlock: FilterDecodeWithConfigBlock<T, C>):
                    WrapCodecWithConfigurableFilterBlock<T, C> = { chain, override ->
                configClass.createConfigInstance().apply(override).let { config ->
                    ValueCodec({ value, buffer ->
                        filterEncodeBlock(value, buffer, config, chain)
                    }, { buffer ->
                        filterDecodeBlock(buffer, config, chain)
                    })
                }
            }
        }
    }

    class FE<T : Any>(private val filterEncodeBlock: FilterEncodeBlock<T>) :
            AndDecodesByDsl<T, FilterDecodeBlock<T>, WrapCodecWithFilterBlock<T>> {
        override infix fun andDecodesBy(
                filterDecodeBlock: FilterDecodeBlock<T>): WrapCodecWithFilterBlock<T> = { chain ->
            ValueCodec({ value, buffer -> filterEncodeBlock(value, buffer, chain) },
                    { buffer -> filterDecodeBlock(buffer, chain) })
        }
    }
}

/**
 * PropertyContext defines the coordinates of a particular property (e.g. in which class the property belongs to)
 */
internal data class PropertyContext(val kClass: KClass<*>, val kProperty: KProperty<*>)

internal class S<T : Any> : DefineSegmentPropertyDsl<T> {
    override infix fun using(buildCodecBlock: BuildCodecBlock<T>) = SC(buildCodecBlock)
    override infix fun <C : Any> using(buildConfigurableCodecBlock: BuildConfigurableCodecBlock<T, C>) =
            SCC(buildConfigurableCodecBlock)

    class SC<T : Any>(private val buildCodecBlock: BuildCodecBlock<T>) : SegmentPropertyProvider<T>() {
        override fun buildCodec() = buildCodecBlock()
    }

    class SCC<T : Any, C : Any>(private val buildCodecBlock: BuildConfigurableCodecBlock<T, C>) :
            SegmentPropertyProvider<T>(), WrappedWithOrConfigWithDsl<T, C> {
        private var overrideConfigBlock: OverrideConfigBlock<C> = {}
        override infix fun withConfig(overrideConfigBlock: OverrideConfigBlock<C>) =
                this.also { this.overrideConfigBlock = overrideConfigBlock }

        override fun buildCodec() = buildCodecBlock(overrideConfigBlock)
    }

    class SF<T : Any>(private val chain: Codec<T>, private val wrapCodecBlock: WrapCodecWithFilterBlock<T>) :
            SegmentPropertyProvider<T>() {
        override fun buildCodec() = wrapCodecBlock(chain)
    }

    class SCF<T : Any, C : Any>(private val codec: Codec<T>,
                                private val wrapCodecBlock: WrapCodecWithConfigurableFilterBlock<T, C>) :
            SegmentPropertyProvider<T>(), WrappedWithOrConfigWithDsl<T, C>, ThenWithOrWithConfigDsl<T, C> {
        private var overrideConfigBlock: OverrideConfigBlock<C> = {}
        override infix fun withConfig(overrideConfigBlock: OverrideConfigBlock<C>) =
                this.also { this.overrideConfigBlock = overrideConfigBlock }

        override fun buildCodec() = wrapCodecBlock(codec, overrideConfigBlock)
    }

    /**
     * A common class which provides a SegmentProperty in any scenario which is possible to terminate a
     * defineSegmentProperty statement
     */
    abstract class SegmentPropertyProvider<T : Any> : WrappedWithDsl<T>, ThenWithDsl<T> {

        override infix fun wrappedWith(wrapCodecBlock: WrapCodecWithFilterBlock<T>) = SF(buildCodec(), wrapCodecBlock)
        override infix fun <C : Any> wrappedWith(wrapCodecBlock: WrapCodecWithConfigurableFilterBlock<T, C>) =
                SCF(buildCodec(), wrapCodecBlock)

        override infix fun thenWith(wrapCodecBlock: WrapCodecWithFilterBlock<T>) = SF(buildCodec(), wrapCodecBlock)
        override infix fun <C : Any> thenWith(wrapCodecBlock: WrapCodecWithConfigurableFilterBlock<T, C>) =
                SCF(buildCodec(), wrapCodecBlock)

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

    override fun thatEncodesBy(encodeSegmentBlock: EncodeSegmentBlock<T>) = SCE(segmentClass, encodeSegmentBlock)

    class SCE<T : Segment>(private val segmentClass: KClass<T>, private val encodeSegmentBlock: EncodeSegmentBlock<T>) :
            AndDecodesByDsl<T, DecodeSegmentBlock<T>, BuildCodecBlock<T>>, BuildCodecBlockBuilder<T>() {
        override fun andDecodesBy(decodeSegmentBlock: DecodeSegmentBlock<T>) =
                createBuildCodecBlock(segmentClass, encodeSegmentBlock, decodeSegmentBlock)
    }

    abstract class BuildCodecBlockBuilder<T : Segment> {
        fun createBuildCodecBlock(segmentClass: KClass<T>, encodeSegmentBlock: EncodeSegmentBlock<T>,
                                  decodeSegmentBlock: DecodeSegmentBlock<T>): BuildCodecBlock<T> {
            validate(segmentClass)
            return C<T>() thatEncodesBy { value, buffer ->
                encodeSegmentBlock(value, SegmentProperties(value.properties), buffer)
            } andDecodesBy { buffer ->
                segmentClass.createSegmentInstance().apply {
                    decodeSegmentBlock(SegmentProperties(this.properties), buffer, this)
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

