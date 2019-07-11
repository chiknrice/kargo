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


internal class ValueCodec<T : Any>(private val encoder: EncoderBlock<T>, private val decoder: DecoderBlock<T>) :
        Codec<T> {
    override fun encode(value: T, buffer: ByteBuffer) = encoder(value, buffer)
    override fun decode(buffer: ByteBuffer) = decoder(buffer)
}

class C<T : Any> internal constructor() {
    infix fun <C : Any> withConfig(configSupplier: ConfigSupplier<C>) = CC<T, C>(configSupplier)
    infix fun withEncoder(encoder: EncoderBlock<T>) = CE(encoder)
    infix fun withDecoder(decoder: DecoderBlock<T>) = CD(decoder)

    class CC<T : Any, C : Any> internal constructor(private val configSupplier: ConfigSupplier<C>) {
        infix fun withEncoder(encoder: EncoderWithConfigBlock<T, C>) =
                CCE(configSupplier, encoder)

        infix fun withDecoder(decoder: DecoderWithConfigBlock<T, C>) =
                CCD(configSupplier, decoder)

        class CCE<T : Any, C : Any> internal constructor(
                private val configSupplier: ConfigSupplier<C>,
                private val encoder: EncoderWithConfigBlock<T, C>
        ) {
            infix fun withDecoder(decoder: DecoderWithConfigBlock<T, C>): ConfigurableCodecFactory<T, C> = { override ->
                configSupplier().apply(override).let { config ->
                    ValueCodec({ value, buffer -> encoder(value, buffer, config) },
                            { buffer -> decoder(buffer, config) })
                }
            }
        }

        class CCD<T : Any, C : Any> internal constructor(
                private val configSupplier: ConfigSupplier<C>,
                private val decoder: DecoderWithConfigBlock<T, C>
        ) {
            infix fun withEncoder(encoder: EncoderWithConfigBlock<T, C>): ConfigurableCodecFactory<T, C> = { override ->
                configSupplier().apply(override).let { config ->
                    ValueCodec({ value, buffer -> encoder(value, buffer, config) },
                            { buffer -> decoder(buffer, config) })
                }
            }
        }
    }

    class CE<T : Any> internal constructor(private val encoder: EncoderBlock<T>) {
        infix fun withDecoder(decoder: DecoderBlock<T>): CodecFactory<T> = { ValueCodec(encoder, decoder) }
    }

    class CD<T : Any> internal constructor(private val decoder: DecoderBlock<T>) {
        infix fun withEncoder(encoder: EncoderBlock<T>): CodecFactory<T> = { ValueCodec(encoder, decoder) }
    }
}

fun <T : Any> codecFor() = C<T>()

class F<T : Any> {
    infix fun <C : Any> withConfig(configSupplier: ConfigSupplier<C>) = FC<T, C>(configSupplier)
    infix fun withEncoder(encoder: EncoderFilterBlock<T>) = FE(encoder)
    infix fun withDecoder(decoder: DecoderFilterBlock<T>) = FD(decoder)

    class FC<T : Any, C : Any> internal constructor(private val configSupplier: ConfigSupplier<C>) {
        infix fun withEncoder(encoder: EncoderFilterWithConfigBlock<T, C>) =
                FCE(configSupplier, encoder)

        infix fun withDecoder(decoder: DecoderFilterWithConfigBlock<T, C>) =
                FCD(configSupplier, decoder)

        class FCE<T : Any, C : Any> internal constructor(
                private val configSupplier: ConfigSupplier<C>,
                private val encoder: EncoderFilterWithConfigBlock<T, C>
        ) {
            infix fun withDecoder(decoder: DecoderFilterWithConfigBlock<T, C>): ConfigurableCodecFilterFactory<T, C> =
                    { codec, override ->
                        configSupplier().apply(override).let { config ->
                            ValueCodec({ value, buffer ->
                                encoder(value, buffer, config, codec)
                            }, { buffer ->
                                decoder(buffer, config, codec)
                            })
                        }
                    }
        }

        class FCD<T : Any, C : Any> internal constructor(
                private val configSupplier: ConfigSupplier<C>,
                private val decoder: DecoderFilterWithConfigBlock<T, C>
        ) {
            infix fun withEncoder(encoder: EncoderFilterWithConfigBlock<T, C>): ConfigurableCodecFilterFactory<T, C> =
                    { codec, override ->
                        configSupplier().apply(override).let { config ->
                            ValueCodec({ value, buffer ->
                                encoder(value, buffer, config, codec)
                            }, { buffer ->
                                decoder(buffer, config, codec)
                            })
                        }
                    }
        }
    }

    class FE<T : Any> internal constructor(private val encoder: EncoderFilterBlock<T>) {
        infix fun withDecoder(decoder: DecoderFilterBlock<T>): CodecFilterFactory<T> = { codec ->
            ValueCodec({ value, buffer -> encoder(value, buffer, codec) }, { buffer -> decoder(buffer, codec) })
        }
    }

    class FD<T : Any> internal constructor(private val decoder: DecoderFilterBlock<T>) {
        infix fun withEncoder(encoder: EncoderFilterBlock<T>): CodecFilterFactory<T> = { codec ->
            ValueCodec({ value, buffer -> encoder(value, buffer, codec) }, { buffer -> decoder(buffer, codec) })
        }
    }
}

fun <T : Any> filterFor() = F<T>()

typealias CodecWrapper<T> = (Codec<T>) -> Codec<T>

class S<T : Any> {

    private lateinit var sc: SC<T>

    fun withCodec(codecFactory: CodecFactory<T>) = SC(codecFactory).also { sc = it }

    fun <C : Any> withCodec(configurableCodecFactory: ConfigurableCodecFactory<T, C>,
                            override: ConfigOverride<C> = {}) = SC {
        configurableCodecFactory(override)
    }.also { sc = it }

    class SC<T : Any> internal constructor(internal val buildCodec: CodecFactory<T>) {

        internal val filters = mutableListOf<CodecWrapper<T>>()

        fun filteredWith(codecFilterFactory: CodecFilterFactory<T>): SC<T> {
            filters.add { codecFilterFactory(it) }
            return this
        }

        fun <C : Any> filteredWith(configurableCodecFilterFactory: ConfigurableCodecFilterFactory<T, C>,
                                   override: ConfigOverride<C> = {}): SC<T> {
            filters.add { configurableCodecFilterFactory(it, override) }
            return this
        }

    }

    internal fun build() = SegmentPropertyProvider {
        if (!::sc.isInitialized) throw ConfigurationException("A segment requires a codec")
        var codec = sc.buildCodec()
        sc.filters.forEach { filter -> codec = filter(codec) }
        codec
    }
}

// TODO: redesign this
fun <T : Any> segment(builder: S<T>.() -> Unit) = S<T>().apply(builder).build()