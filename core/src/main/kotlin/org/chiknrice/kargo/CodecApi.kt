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

/**
 * The contract of a codec for value of type T
 */
interface Codec<T : Any> {
    /**
     * Encode a value of type T to a ByteBuffer
     */
    fun encode(value: T, buffer: ByteBuffer)

    /**
     * Decode a value of type T from a ByteBuffer
     */
    fun decode(buffer: ByteBuffer): T
}

/**
 * The contract of encoding a value of type T based on a configuration of type C to a ByteBuffer
 */
typealias EncoderWithConfigBlock<T, C> = (value: T, buffer: ByteBuffer, config: C) -> Unit

/**
 * The contract of decoding a value of type T based on a configuration of type C from a ByteBuffer
 */
typealias DecoderWithConfigBlock<T, C> = (buffer: ByteBuffer, config: C) -> T

/**
 * The contract of encoding a value of type T to a ByteBuffer
 */
typealias EncoderBlock<T> = (value: T, buffer: ByteBuffer) -> Unit

/**
 * The contract of decoding a value of type T from a ByteBuffer
 */
typealias DecoderBlock<T> = (buffer: ByteBuffer) -> T

/**
 * The contract of filtering a codec when encoding a value of type T based on a configuration of type C to a ByteBuffer
 */
typealias EncoderFilterWithConfigBlock<T, C> = (value: T, buffer: ByteBuffer, config: C, chain: Codec<T>) -> Unit

/**
 * The contract of filtering a codec when decoding a value of type T based on a configuration of type C from a ByteBuffer
 */
typealias DecoderFilterWithConfigBlock<T, C> = (buffer: ByteBuffer, config: C, chain: Codec<T>) -> T

/**
 * The contract of filtering a codec when encoding a value of type T to a ByteBuffer
 */
typealias EncoderFilterBlock<T> = (value: T, buffer: ByteBuffer, chain: Codec<T>) -> Unit

/**
 * The contract of filtering a codec when decoding a value of type T from a ByteBuffer
 */
typealias DecoderFilterBlock<T> = (buffer: ByteBuffer, chain: Codec<T>) -> T

/**
 * The contract of building a configurable codec for type T with an override of configuration of type C
 */
typealias ConfigurableCodecFactory<T, C> = (override: ConfigOverride<C>) -> Codec<T>

/**
 * The contract of building a codec for type T
 */
typealias CodecFactory<T> = () -> Codec<T>

/**
 * The contract of building a configurable filter of a codec for type T with an override of filter configuration of type C
 */
typealias ConfigurableCodecFilterFactory<T, C> = (chain: Codec<T>, override: ConfigOverride<C>) -> Codec<T>

/**
 * The contract of building a filter of a codec for type T
 */
typealias CodecFilterFactory<T> = (chain: Codec<T>) -> Codec<T>

/**
 * The contract of supplying a default configuration of type C
 */
typealias ConfigSupplier<C> = () -> C

/**
 * The contract of (possibly) overriding defaults of a configuration of type C
 */
typealias ConfigOverride<C> = C.() -> Unit
