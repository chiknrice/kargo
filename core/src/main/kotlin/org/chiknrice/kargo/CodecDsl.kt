/*
 * Copyright (c) 2019 Ian Bondoc
 *
 * This file is part of project "kargo"
 *
 * Project kargo is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or(at your option) any later version.
 *
 * Project kargo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 */

package org.chiknrice.kargo

import java.nio.ByteBuffer

interface Encoder<T> {
    fun encode(value: T, buffer: ByteBuffer, context: CodecContext)
}

interface Decoder<T> {
    fun decode(buffer: ByteBuffer, context: CodecContext): T
}

interface Codec<T> : Encoder<T>, Decoder<T>

internal typealias EncodeBlock<T> = (value: T, buffer: ByteBuffer, context: CodecContext) -> Unit

internal typealias DecodeBlock<T> = (buffer: ByteBuffer, context: CodecContext) -> T

internal class ValueCodec<T>(
        private val encodeBlock: EncodeBlock<T>,
        private val decodeBlock: DecodeBlock<T>
) : Codec<T> {

    override fun encode(value: T, buffer: ByteBuffer, context: CodecContext) =
            encodeBlock(value, buffer, context)

    override fun decode(buffer: ByteBuffer, context: CodecContext) =
            decodeBlock(buffer, context)

}

class CodecBuilder<T : Any> internal constructor() {

    private lateinit var encodeBlock: EncodeBlock<T>
    private lateinit var decodeBlock: DecodeBlock<T>

    fun onEncode(block: EncodeBlock<T>) {
        encodeBlock = block
    }

    fun onDecode(block: DecodeBlock<T>) {
        decodeBlock = block
    }

    internal fun build(): Codec<T> {
        if (!::encodeBlock.isInitialized || !::decodeBlock.isInitialized) {
            throw ConfigurationException("Incomplete codec definition")
        }
        return ValueCodec(encodeBlock, decodeBlock)
    }

}

fun <T : Any> codec(block: CodecBuilder<T>.() -> Unit) =
        CodecBuilder<T>().apply(block).build()
