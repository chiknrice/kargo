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

/**
 * The contract of what can be done after defineCodec function
 */
interface DefineCodecDsl<T : Any> :
        WithEncodeOrWithDecodeDsl<T, EncoderBlock<T>, DecoderBlock<T>, CodecFactory<T>>,
        CodecWithConfigDsl<T>

/**
 * The contract of what can be done after defineCodecFilter function
 */
interface DefineCodecFilterDsl<T : Any> :
        WithEncodeOrWithDecodeDsl<T, EncoderFilterBlock<T>, DecoderFilterBlock<T>, CodecFilterFactory<T>>,
        CodecFilterWithConfigDsl<T>

/**
 * The contract providing the option to define an encoder or a decoder
 */
interface WithEncodeOrWithDecodeDsl<T : Any, E, D, R> :
        WithEncoderDsl<T, E, WithDecoderDsl<T, D, R>>,
        WithDecoderDsl<T, D, WithEncoderDsl<T, E, R>>

/**
 * The contract of defining a configuration for a codec
 */
interface CodecWithConfigDsl<T : Any> {
    infix fun <C : Any> withConfig(configSupplier: ConfigSupplier<C>):
            ConfigurableCodecDsl<T, C>
}

typealias ConfigurableCodecDsl<T, C> =
        WithEncodeOrWithDecodeDsl<T,
                EncoderWithConfigBlock<T, C>,
                DecoderWithConfigBlock<T, C>,
                ConfigurableCodecFactory<T, C>>

/**
 * The contract of defining a configuration for a codec filter
 */
interface CodecFilterWithConfigDsl<T : Any> {
    infix fun <C : Any> withConfig(configSupplier: ConfigSupplier<C>):
            ConfigurableCodecFilterDsl<T, C>
}

typealias ConfigurableCodecFilterDsl<T, C> =
        WithEncodeOrWithDecodeDsl<T,
                EncoderFilterWithConfigBlock<T, C>,
                DecoderFilterWithConfigBlock<T, C>,
                ConfigurableCodecFilterFactory<T, C>>

/**
 * The contract of defining an encoder
 */
interface WithEncoderDsl<T : Any, P, R> {
    infix fun withEncoder(encoder: P): R
}

/**
 * The contract of defining a decoder
 */
interface WithDecoderDsl<T : Any, P, R> {
    infix fun withDecoder(decoder: P): R
}

/**
 * The contract of what can be done after defineSegment function
 */
interface DefineSegmentDsl<T : Any> {
    infix fun using(codecFactory: CodecFactory<T>): FilterDsl<T>
    infix fun <C : Any> using(configurableCodecFactory: ConfigurableCodecFactory<T, C>): FilterOrConfigDsl<T, C>
}

/**
 * The contract of providing an option to define config override together with filtering
 */
interface FilterOrConfigDsl<T : Any, C : Any> : FilterDsl<T> {
    infix fun withOverride(override: ConfigOverride<C>): FilterDsl<T>
}

/**
 * The contract of providing the option to filter with or without a configuration
 */
interface FilterDsl<T : Any> {
    infix fun filterWith(codecFilterFactory: CodecFilterFactory<T>): FilterDsl<T>
    infix fun <C : Any> filterWith(
            configurableCodecFilterFactory: ConfigurableCodecFilterFactory<T, C>): FilterOrConfigDsl<T, C>
}

/**
 * The entrypoint function of defining a codec
 */
fun <T : Any> defineCodec(): DefineCodecDsl<T> = C()

/**
 * The entrypoint function of defining a codec filter
 */
fun <T : Any> defineFilter(): DefineCodecFilterDsl<T> = F()

/**
 * The entrypoint function of defining a segment property
 */
fun <T : Any> defineSegment(): DefineSegmentDsl<T> = S()
