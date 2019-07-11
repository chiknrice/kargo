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

typealias EncoderWithConfigBlock<T, C> = (value: T, buffer: ByteBuffer, config: C) -> Unit
typealias DecoderWithConfigBlock<T, C> = (buffer: ByteBuffer, config: C) -> T
typealias EncoderBlock<T> = (value: T, buffer: ByteBuffer) -> Unit
typealias DecoderBlock<T> = (buffer: ByteBuffer) -> T

typealias EncoderFilterWithConfigBlock<T, C> = (value: T, buffer: ByteBuffer, config: C, chain: Codec<T>) -> Unit
typealias DecoderFilterWithConfigBlock<T, C> = (buffer: ByteBuffer, config: C, chain: Codec<T>) -> T
typealias EncoderFilterBlock<T> = (value: T, buffer: ByteBuffer, chain: Codec<T>) -> Unit
typealias DecoderFilterBlock<T> = (buffer: ByteBuffer, chain: Codec<T>) -> T

typealias ConfigurableCodecFactory<T, C> = (override: ConfigOverride<C>) -> Codec<T>
typealias CodecFactory<T> = () -> Codec<T>
typealias ConfigurableCodecFilterFactory<T, C> = (chain: Codec<T>, override: ConfigOverride<C>) -> Codec<T>
typealias CodecFilterFactory<T> = (chain: Codec<T>) -> Codec<T>

typealias ConfigSupplier<C> = () -> C
typealias ConfigOverride<C> = C.() -> Unit

interface Codec<T : Any> {
    fun encode(value: T, buffer: ByteBuffer)
    fun decode(buffer: ByteBuffer): T
}