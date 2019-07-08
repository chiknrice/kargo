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

internal typealias EncodeFilterBlock<T> = (value: T, buffer: ByteBuffer, context: CodecContext, chain: Encoder<T>) -> Unit

internal typealias DecodeFilterBlock<T> = (buffer: ByteBuffer, context: CodecContext, chain: Decoder<T>) -> T

internal class CodecFilter<T>(
        private val encodeFilterBlock: EncodeFilterBlock<T>,
        private val decodeFilterBlock: DecodeFilterBlock<T>,
        private val chain: Codec<T>
) : Codec<T> {

    override fun decode(buffer: ByteBuffer, context: CodecContext): T =
            decodeFilterBlock(buffer, context, chain)

    override fun encode(value: T, buffer: ByteBuffer, context: CodecContext) =
            encodeFilterBlock(value, buffer, context, chain)

}

internal typealias FilterWrappingSpec<T> = (chain: Codec<T>) -> Codec<T>

fun <T> Codec<T>.filterWith(wrap: FilterWrappingSpec<T>) = wrap(this)

class CodecFilterBuilder<T> internal constructor() {

    private lateinit var encodeFilterBlock: EncodeFilterBlock<T>
    private lateinit var decodeFilterBlock: DecodeFilterBlock<T>

    fun onEncodeFilter(block: EncodeFilterBlock<T>) {
        encodeFilterBlock = block
    }

    fun onDecodeFilter(block: DecodeFilterBlock<T>) {
        decodeFilterBlock = block
    }

    internal fun build(): FilterWrappingSpec<T> {
        if (!::encodeFilterBlock.isInitialized || !::decodeFilterBlock.isInitialized) {
            throw ConfigurationException("Incomplete codec filter definition")
        }

        return { chain -> CodecFilter(encodeFilterBlock, decodeFilterBlock, chain) }
    }

}

fun <T : Any> filter(block: CodecFilterBuilder<T>.() -> Unit) =
        CodecFilterBuilder<T>().apply(block).build()
