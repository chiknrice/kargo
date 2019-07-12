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

internal class ValueCodec<T : Any>(private val encoder: EncoderBlock<T>, private val decoder: DecoderBlock<T>) :
        Codec<T> {
    override fun encode(value: T, buffer: ByteBuffer) = encoder(value, buffer)
    override fun decode(buffer: ByteBuffer) = decoder(buffer)
}

internal class C<T : Any> : DefineCodecDsl<T> {
    override infix fun <C : Any> withConfig(configSupplier: ConfigSupplier<C>) = CC<T, C>(configSupplier)
    override infix fun withEncoder(encoder: EncoderBlock<T>) = CE(encoder)
    override infix fun withDecoder(decoder: DecoderBlock<T>) = CD(decoder)

    class CC<T : Any, C : Any>(private val configSupplier: ConfigSupplier<C>) :
            ConfigurableCodecDsl<T, C> {
        override infix fun withEncoder(encoder: EncoderWithConfigBlock<T, C>) =
                CCE(configSupplier, encoder)

        override infix fun withDecoder(decoder: DecoderWithConfigBlock<T, C>) =
                CCD(configSupplier, decoder)

        class CCE<T : Any, C : Any>(private val configSupplier: ConfigSupplier<C>,
                                    private val encoder: EncoderWithConfigBlock<T, C>
        ) : WithDecoderDsl<T, DecoderWithConfigBlock<T, C>, ConfigurableCodecFactory<T, C>> {
            override infix fun withDecoder(
                    decoder: DecoderWithConfigBlock<T, C>): ConfigurableCodecFactory<T, C> = { override ->
                configSupplier().apply(override).let { config ->
                    ValueCodec({ value, buffer -> encoder(value, buffer, config) },
                            { buffer -> decoder(buffer, config) })
                }
            }
        }

        class CCD<T : Any, C : Any>(private val configSupplier: ConfigSupplier<C>,
                                    private val decoder: DecoderWithConfigBlock<T, C>
        ) : WithEncoderDsl<T, EncoderWithConfigBlock<T, C>, ConfigurableCodecFactory<T, C>> {
            override infix fun withEncoder(
                    encoder: EncoderWithConfigBlock<T, C>): ConfigurableCodecFactory<T, C> = { override ->
                configSupplier().apply(override).let { config ->
                    ValueCodec({ value, buffer -> encoder(value, buffer, config) },
                            { buffer -> decoder(buffer, config) })
                }
            }
        }
    }

    class CE<T : Any>(private val encoder: EncoderBlock<T>) :
            WithDecoderDsl<T, DecoderBlock<T>, CodecFactory<T>> {
        override infix fun withDecoder(decoder: DecoderBlock<T>): CodecFactory<T> = { ValueCodec(encoder, decoder) }
    }

    class CD<T : Any>(private val decoder: DecoderBlock<T>) :
            WithEncoderDsl<T, EncoderBlock<T>, CodecFactory<T>> {
        override infix fun withEncoder(encoder: EncoderBlock<T>): CodecFactory<T> = { ValueCodec(encoder, decoder) }
    }
}

internal class F<T : Any> : DefineCodecFilterDsl<T> {
    override infix fun <C : Any> withConfig(configSupplier: ConfigSupplier<C>) = FC<T, C>(configSupplier)
    override infix fun withEncoder(encoderFilter: EncoderFilterBlock<T>) = FE(encoderFilter)
    override infix fun withDecoder(decoderFilter: DecoderFilterBlock<T>) = FD(decoderFilter)

    class FC<T : Any, C : Any>(private val configSupplier: ConfigSupplier<C>) : ConfigurableCodecFilterDsl<T, C> {
        override infix fun withEncoder(encoderFilter: EncoderFilterWithConfigBlock<T, C>) = FCE(configSupplier,
                encoderFilter)

        override infix fun withDecoder(decoderFilter: DecoderFilterWithConfigBlock<T, C>) = FCD(configSupplier,
                decoderFilter)

        class FCE<T : Any, C : Any>(private val configSupplier: ConfigSupplier<C>,
                                    private val encoderFilter: EncoderFilterWithConfigBlock<T, C>
        ) : WithDecoderDsl<T, DecoderFilterWithConfigBlock<T, C>, ConfigurableCodecFilterFactory<T, C>> {
            override infix fun withDecoder(
                    decoderFilter: DecoderFilterWithConfigBlock<T, C>): ConfigurableCodecFilterFactory<T, C> =
                    { codec, override ->
                        configSupplier().apply(override).let { config ->
                            ValueCodec({ value, buffer ->
                                encoderFilter(value, buffer, config, codec)
                            }, { buffer ->
                                decoderFilter(buffer, config, codec)
                            })
                        }
                    }
        }

        class FCD<T : Any, C : Any>(private val configSupplier: ConfigSupplier<C>,
                                    private val decoderFilter: DecoderFilterWithConfigBlock<T, C>
        ) : WithEncoderDsl<T, EncoderFilterWithConfigBlock<T, C>, ConfigurableCodecFilterFactory<T, C>> {
            override infix fun withEncoder(
                    encoderFilter: EncoderFilterWithConfigBlock<T, C>): ConfigurableCodecFilterFactory<T, C> =
                    { codec, override ->
                        configSupplier().apply(override).let { config ->
                            ValueCodec({ value, buffer ->
                                encoderFilter(value, buffer, config, codec)
                            }, { buffer ->
                                decoderFilter(buffer, config, codec)
                            })
                        }
                    }
        }
    }

    class FE<T : Any>(private val encoderFilter: EncoderFilterBlock<T>) :
            WithDecoderDsl<T, DecoderFilterBlock<T>, CodecFilterFactory<T>> {
        override infix fun withDecoder(decoderFilter: DecoderFilterBlock<T>): CodecFilterFactory<T> = { codec ->
            ValueCodec({ value, buffer -> encoderFilter(value, buffer, codec) },
                    { buffer -> decoderFilter(buffer, codec) })
        }
    }

    class FD<T : Any>(private val decoderFilter: DecoderFilterBlock<T>) :
            WithEncoderDsl<T, EncoderFilterBlock<T>, CodecFilterFactory<T>> {
        override infix fun withEncoder(encoderFilter: EncoderFilterBlock<T>): CodecFilterFactory<T> = { codec ->
            ValueCodec({ value, buffer -> encoderFilter(value, buffer, codec) },
                    { buffer -> decoderFilter(buffer, codec) })
        }
    }
}

internal class S<T : Any> : DefineSegmentDsl<T> {

    override infix fun using(codecFactory: CodecFactory<T>): FilterDsl<T> = SC(codecFactory)
    override infix fun <C : Any> using(configurableCodecFactory: ConfigurableCodecFactory<T, C>) = SCC(
            configurableCodecFactory)

    class SC<T : Any>(private val codecFactory: CodecFactory<T>) : SegmentPropertyProvider<T>() {
        override fun buildCodec(): Codec<T> = codecFactory()
    }

    class SCC<T : Any, C : Any>(private val configurableCodecFactory: ConfigurableCodecFactory<T, C>) :
            SegmentPropertyProvider<T>(), FilterOrConfigDsl<T, C> {
        private var override: ConfigOverride<C> = {}
        override infix fun withOverride(
                override: ConfigOverride<C>): FilterDsl<T> = this.also { this.override = override }

        override fun buildCodec(): Codec<T> = configurableCodecFactory(override)
    }

    class SF<T : Any>(private val codec: Codec<T>, private val codecFilterFactory: CodecFilterFactory<T>) :
            SegmentPropertyProvider<T>() {
        override fun buildCodec(): Codec<T> = codecFilterFactory(codec)
    }

    class SCF<T : Any, C : Any>(private val codec: Codec<T>,
                                private val configurableCodecFilterFactory: ConfigurableCodecFilterFactory<T, C>) :
            SegmentPropertyProvider<T>(), FilterOrConfigDsl<T, C> {
        private var override: ConfigOverride<C> = {}
        override infix fun withOverride(override: ConfigOverride<C>): FilterDsl<T> =
                this.also { this.override = override }

        override fun buildCodec(): Codec<T> = configurableCodecFilterFactory(codec, override)
    }

    data class PropertyContext(val kClass: KClass<*>, val kProperty: KProperty<*>)

    abstract class SegmentPropertyProvider<T : Any> : FilterDsl<T> {

        override infix fun filterWith(codecFilterFactory: CodecFilterFactory<T>): FilterDsl<T> = SF(buildCodec(),
                codecFilterFactory)

        override infix fun <C : Any> filterWith(
                configurableCodecFilterFactory: ConfigurableCodecFilterFactory<T, C>): FilterOrConfigDsl<T, C> = SCF(
                buildCodec(), configurableCodecFilterFactory)

        protected abstract fun buildCodec(): Codec<T>

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