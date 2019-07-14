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
 * The contract of an encoder on how to encode a value of type T to a ByteBuffer
 */
interface Encoder<T> {
    fun encode(value: T, buffer: ByteBuffer)
}

/**
 * The contract of a decoder on how to decode a value of type T from a ByteBuffer
 */
interface Decoder<T> {
    fun decode(buffer: ByteBuffer): T
}

/**
 * The contract of a codec for value of type T
 */
interface Codec<T : Any> : Encoder<T>, Decoder<T>

/**
 * The contract of encoding a value of type T based on a configuration of type C to a ByteBuffer
 */
typealias EncodeWithConfigBlock<T, C> = (value: T, buffer: ByteBuffer, config: C) -> Unit

/**
 * The contract of decoding a value of type T based on a configuration of type C from a ByteBuffer
 */
typealias DecodeWithConfigBlock<T, C> = (buffer: ByteBuffer, config: C) -> T

/**
 * The contract of encoding a value of type T to a ByteBuffer
 */
typealias EncodeBlock<T> = (value: T, buffer: ByteBuffer) -> Unit

/**
 * The contract of decoding a value of type T from a ByteBuffer
 */
typealias DecodeBlock<T> = (buffer: ByteBuffer) -> T

/**
 * The contract of encoding a segment of type T with the list of segment properties to a ByteBuffer
 */
typealias EncodeSegmentBlock<T> = (segment: T, segmentProperties: SegmentProperties, buffer: ByteBuffer) -> Unit

/**
 * The contract of decoding a (newly created) segment of type T with the list of segment properties from a ByteBuffer
 */
typealias DecodeSegmentBlock<T> = (segmentProperties: SegmentProperties, buffer: ByteBuffer, newSegment: T) -> Unit

/**
 * The contract of filtering an encoder when encoding a value of type T based on a configuration of type C
 */
typealias FilterEncodeWithConfigBlock<T, C> = (value: T, buffer: ByteBuffer, config: C, chain: Encoder<T>) -> Unit

/**
 * The contract of filtering a decoder when decoding a value of type T based on a configuration of type C
 */
typealias FilterDecodeWithConfigBlock<T, C> = (buffer: ByteBuffer, config: C, chain: Decoder<T>) -> T

/**
 * The contract of filtering an encoder when encoding a value of type T
 */
typealias FilterEncodeBlock<T> = (value: T, buffer: ByteBuffer, chain: Encoder<T>) -> Unit

/**
 * The contract of filtering a decoder when decoding a value of type T
 */
typealias FilterDecodeBlock<T> = (buffer: ByteBuffer, chain: Decoder<T>) -> T

/**
 * The contract of building a configurable codec for type T with an override of configuration of type C
 */
typealias BuildConfigurableCodecBlock<T, C> = (override: OverrideConfigBlock<C>) -> Codec<T>

/**
 * The contract of building a codec for type T
 */
typealias BuildCodecBlock<T> = () -> Codec<T>

/**
 * The contract of building a configurable filter of a codec for type T with an override of filter configuration of type C
 */
typealias WrapCodecWithConfigurableFilterBlock<T, C> = (chain: Codec<T>, override: OverrideConfigBlock<C>) -> Codec<T>

/**
 * The contract of building a filter of a codec for type T
 */
typealias WrapCodecWithFilterBlock<T> = (chain: Codec<T>) -> Codec<T>

/**
 * The contract of (possibly) overriding defaults of a configuration of type C
 */
typealias OverrideConfigBlock<C> = C.() -> Unit
