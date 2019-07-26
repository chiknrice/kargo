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
typealias ConfigurableEncodeSpec<T, C> = (value: T, buffer: ByteBuffer, config: C) -> Unit

/**
 * The contract of decoding a value of type T based on a configuration of type C from a ByteBuffer
 */
typealias ConfigurableDecodeSpec<T, C> = (buffer: ByteBuffer, config: C) -> T

/**
 * The contract of encoding a value of type T to a ByteBuffer
 */
typealias EncodeSpec<T> = (value: T, buffer: ByteBuffer) -> Unit

/**
 * The contract of decoding a value of type T from a ByteBuffer
 */
typealias DecodeSpec<T> = (buffer: ByteBuffer) -> T

/**
 * The contract of encoding a segment of type T with the list of segment properties to a ByteBuffer
 */
typealias EncodeSegmentSpec<T> = (segment: T, segmentProperties: SegmentProperties, buffer: ByteBuffer) -> Unit

/**
 * The contract of decoding a (newly created) segment of type T with the list of segment properties from a ByteBuffer
 */
typealias DecodeSegmentSpec<T> = (segmentProperties: SegmentProperties, buffer: ByteBuffer, newSegment: T) -> Unit

/**
 * The contract of filtering an encoder when encoding a value of type T based on a configuration of type C
 */
typealias ConfigurableFilterEncodeSpec<T, C> = (value: T, buffer: ByteBuffer, config: C, chain: Encoder<T>) -> Unit

/**
 * The contract of filtering a decoder when decoding a value of type T based on a configuration of type C
 */
typealias ConfigurableFilterDecodeSpec<T, C> = (buffer: ByteBuffer, config: C, chain: Decoder<T>) -> T

/**
 * The contract of filtering an encoder when encoding a value of type T
 */
typealias FilterEncodeSpec<T> = (value: T, buffer: ByteBuffer, chain: Encoder<T>) -> Unit

/**
 * The contract of filtering a decoder when decoding a value of type T
 */
typealias FilterDecodeSpec<T> = (buffer: ByteBuffer, chain: Decoder<T>) -> T

/**
 * The contract of building a codec for type T
 */
interface CodecDefinition<T : Any> {
    fun buildCodec(): Codec<T>
}

/**
 * The contract of optionally overriding configuration of type C of a codec for type T
 */
interface ConfigurableCodecDefinition<T : Any, C : Any> : CodecDefinition<T> {
    fun withOverrides(overrides: ConfigSpec<C>): ConfigurableCodecDefinition<T, C>
}

/**
 * The contract of filtering a codec for type T
 */
interface FilterDefinition<T : Any> {
    fun wrapCodec(chain: Codec<T>): Codec<T>
}

/**
 * The contract of optionally overriding configuration of type C of a filter for type T
 */
interface ConfigurableFilterDefinition<T : Any, C : Any> : FilterDefinition<T> {
    fun withOverrides(overrides: ConfigSpec<C>): ConfigurableFilterDefinition<T, C>
}

/**
 * The contract of configuring an object of type C
 */
typealias ConfigSpec<C> = C.() -> Unit

/**
 * Generic exception to represent any exceptional cases during encoding or decoding
 */
class CodecException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class CodecConfigurationException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}