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

internal class ValueCodec<T : Any>(private val encodeBlock: EncodeBlock<T>, private val decodeBlock: DecodeBlock<T>) :
        Codec<T> {
    override fun encode(value: T, buffer: ByteBuffer) = encodeBlock(value, buffer)
    override fun decode(buffer: ByteBuffer) = decodeBlock(buffer)
}

internal class C<T : Any> : DefineCodecDsl<T> {
    override infix fun <C : Any> withConfig(configSupplier: SupplyDefaultConfigBlock<C>) = CC<T, C>(configSupplier)
    override infix fun withEncoder(encoder: EncodeBlock<T>) = CE(encoder)
    override infix fun withDecoder(decoder: DecodeBlock<T>) = CD(decoder)

    class CC<T : Any, C : Any>(private val supplyDefaultConfigBlock: SupplyDefaultConfigBlock<C>) :
            ConfigurableCodecDsl<T, C> {
        override infix fun withEncoder(encoder: EncodeWithConfigBlock<T, C>) =
                CCE(supplyDefaultConfigBlock, encoder)

        override infix fun withDecoder(decoder: DecodeWithConfigBlock<T, C>) =
                CCD(supplyDefaultConfigBlock, decoder)

        class CCE<T : Any, C : Any>(private val supplyDefaultConfigBlock: SupplyDefaultConfigBlock<C>,
                                    private val encodeWithConfigBlock: EncodeWithConfigBlock<T, C>
        ) : WithDecoderDsl<T, DecodeWithConfigBlock<T, C>, BuildCodecWithOverrideBlock<T, C>> {
            override infix fun withDecoder(
                    decodeWithConfigBlock: DecodeWithConfigBlock<T, C>): BuildCodecWithOverrideBlock<T, C> = { override ->
                supplyDefaultConfigBlock().apply(override).let { config ->
                    ValueCodec({ value, buffer -> encodeWithConfigBlock(value, buffer, config) },
                            { buffer -> decodeWithConfigBlock(buffer, config) })
                }
            }
        }

        class CCD<T : Any, C : Any>(private val supplyDefaultConfigBlock: SupplyDefaultConfigBlock<C>,
                                    private val decodeWithConfigBlock: DecodeWithConfigBlock<T, C>
        ) : WithEncoderDsl<T, EncodeWithConfigBlock<T, C>, BuildCodecWithOverrideBlock<T, C>> {
            override infix fun withEncoder(
                    encodeWithConfigBlock: EncodeWithConfigBlock<T, C>): BuildCodecWithOverrideBlock<T, C> = { override ->
                supplyDefaultConfigBlock().apply(override).let { config ->
                    ValueCodec({ value, buffer -> encodeWithConfigBlock(value, buffer, config) },
                            { buffer -> decodeWithConfigBlock(buffer, config) })
                }
            }
        }
    }

    class CE<T : Any>(private val encodeBlock: EncodeBlock<T>) :
            WithDecoderDsl<T, DecodeBlock<T>, BuildCodecBlock<T>> {
        override infix fun withDecoder(decodeBlock: DecodeBlock<T>): BuildCodecBlock<T> = {
            ValueCodec(encodeBlock, decodeBlock)
        }
    }

    class CD<T : Any>(private val decodeBlock: DecodeBlock<T>) :
            WithEncoderDsl<T, EncodeBlock<T>, BuildCodecBlock<T>> {
        override infix fun withEncoder(encodeBlock: EncodeBlock<T>): BuildCodecBlock<T> = {
            ValueCodec(encodeBlock, decodeBlock)
        }
    }
}

internal class F<T : Any> : DefineCodecFilterDsl<T> {
    override infix fun <C : Any> withConfig(supplyDefaultConfigBlock: SupplyDefaultConfigBlock<C>) =
            FC<T, C>(supplyDefaultConfigBlock)

    override infix fun withEncoder(filterEncodeBlock: FilterEncodeBlock<T>) = FE(filterEncodeBlock)
    override infix fun withDecoder(filterDecodeBlock: FilterDecodeBlock<T>) = FD(filterDecodeBlock)

    class FC<T : Any, C : Any>(private val supplyDefaultConfigBlock: SupplyDefaultConfigBlock<C>) :
            ConfigurableCodecFilterDsl<T, C> {
        override infix fun withEncoder(filterEncodeWithConfigBlock: FilterEncodeWithConfigBlock<T, C>) =
                FCE(supplyDefaultConfigBlock, filterEncodeWithConfigBlock)

        override infix fun withDecoder(filterDecodeWithConfigBlock: FilterDecodeWithConfigBlock<T, C>) =
                FCD(supplyDefaultConfigBlock, filterDecodeWithConfigBlock)

        class FCE<T : Any, C : Any>(private val supplyDefaultConfigBlock: SupplyDefaultConfigBlock<C>,
                                    private val filterEncodeWithConfigBlock: FilterEncodeWithConfigBlock<T, C>
        ) : WithDecoderDsl<T, FilterDecodeWithConfigBlock<T, C>, BuildFilteredCodecWithConfigBlock<T, C>> {
            override infix fun withDecoder(filterDecodeWithConfigBlock: FilterDecodeWithConfigBlock<T, C>):
                    BuildFilteredCodecWithConfigBlock<T, C> = { chain, override ->
                supplyDefaultConfigBlock().apply(override).let { config ->
                    ValueCodec({ value, buffer ->
                        filterEncodeWithConfigBlock(value, buffer, config, chain)
                    }, { buffer ->
                        filterDecodeWithConfigBlock(buffer, config, chain)
                    })
                }
            }
        }

        class FCD<T : Any, C : Any>(private val supplyDefaultConfigBlock: SupplyDefaultConfigBlock<C>,
                                    private val filterDecodeWithConfigBlock: FilterDecodeWithConfigBlock<T, C>
        ) : WithEncoderDsl<T, FilterEncodeWithConfigBlock<T, C>, BuildFilteredCodecWithConfigBlock<T, C>> {
            override infix fun withEncoder(filterEncodeWithConfigBlock: FilterEncodeWithConfigBlock<T, C>):
                    BuildFilteredCodecWithConfigBlock<T, C> = { chain, override ->
                supplyDefaultConfigBlock().apply(override).let { config ->
                    ValueCodec({ value, buffer ->
                        filterEncodeWithConfigBlock(value, buffer, config, chain)
                    }, { buffer ->
                        filterDecodeWithConfigBlock(buffer, config, chain)
                    })
                }
            }
        }
    }

    class FE<T : Any>(private val filterEncodeBlock: FilterEncodeBlock<T>) :
            WithDecoderDsl<T, FilterDecodeBlock<T>, BuildFilteredCodecBlock<T>> {
        override infix fun withDecoder(filterDecodeBlock: FilterDecodeBlock<T>): BuildFilteredCodecBlock<T> = { chain ->
            ValueCodec({ value, buffer -> filterEncodeBlock(value, buffer, chain) },
                    { buffer -> filterDecodeBlock(buffer, chain) })
        }
    }

    class FD<T : Any>(private val filterDecodeBlock: FilterDecodeBlock<T>) :
            WithEncoderDsl<T, FilterEncodeBlock<T>, BuildFilteredCodecBlock<T>> {
        override infix fun withEncoder(filterEncodeBlock: FilterEncodeBlock<T>): BuildFilteredCodecBlock<T> = { chain ->
            ValueCodec({ value, buffer -> filterEncodeBlock(value, buffer, chain) },
                    { buffer -> filterDecodeBlock(buffer, chain) })
        }
    }
}

internal class S<T : Any> : DefineSegmentDsl<T> {
    override infix fun using(buildCodecBlock: BuildCodecBlock<T>) = SC(buildCodecBlock)
    override infix fun <C : Any> using(buildCodecWithOverrideBlock: BuildCodecWithOverrideBlock<T, C>) =
            SCC(buildCodecWithOverrideBlock)

    class SC<T : Any>(private val buildCodecBlock: BuildCodecBlock<T>) : SegmentPropertyProvider<T>() {
        override fun buildCodec() = buildCodecBlock()
    }

    class SCC<T : Any, C : Any>(private val buildCodecWithOverrideBlock: BuildCodecWithOverrideBlock<T, C>) :
            SegmentPropertyProvider<T>(), FilterOrConfigDsl<T, C> {
        private var overrideConfigBlock: OverrideConfigBlock<C> = {}
        override infix fun withOverride(overrideConfigBlock: OverrideConfigBlock<C>) =
                this.also { this.overrideConfigBlock = overrideConfigBlock }

        override fun buildCodec() = buildCodecWithOverrideBlock(overrideConfigBlock)
    }

    class SF<T : Any>(private val chain: Codec<T>, private val buildFilteredCodecBlock: BuildFilteredCodecBlock<T>) :
            SegmentPropertyProvider<T>() {
        override fun buildCodec() = buildFilteredCodecBlock(chain)
    }

    class SCF<T : Any, C : Any>(private val codec: Codec<T>,
                                private val buildFilteredCodecWithConfigBlock: BuildFilteredCodecWithConfigBlock<T, C>) :
            SegmentPropertyProvider<T>(), FilterOrConfigDsl<T, C> {
        private var overrideConfigBlock: OverrideConfigBlock<C> = {}
        override infix fun withOverride(overrideConfigBlock: OverrideConfigBlock<C>) =
                this.also { this.overrideConfigBlock = overrideConfigBlock }

        override fun buildCodec() = buildFilteredCodecWithConfigBlock(codec, overrideConfigBlock)
    }

    data class PropertyContext(val kClass: KClass<*>, val kProperty: KProperty<*>)

    abstract class SegmentPropertyProvider<T : Any> : FilterDsl<T> {

        override infix fun filterWith(buildFilteredCodecBlock: BuildFilteredCodecBlock<T>) =
                SF(buildCodec(), buildFilteredCodecBlock)

        override infix fun <C : Any> filterWith(
                buildFilteredCodecWithConfigBlock: BuildFilteredCodecWithConfigBlock<T, C>) =
                SCF(buildCodec(), buildFilteredCodecWithConfigBlock)

        abstract fun buildCodec(): Codec<T>

        operator fun provideDelegate(
                thisRef: Segment,
                property: KProperty<*>
        ): ReadWriteProperty<Segment, T?> {
            val codec = S.propertyCodecs.computeIfAbsent(PropertyContext(thisRef::class, property)) {
                buildCodec()
            } as Codec<T>
            return SegmentProperty(property, codec).also {
                thisRef.internalProperties.add(it)
            }
        }
    }

    companion object {
        val propertyCodecs = mutableMapOf<PropertyContext, Codec<*>>()
    }

}