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

import kotlin.reflect.KClass

/**
 * The contract of what can be done after defineCodec function
 */
interface DefineCodecDsl<T : Any> :
        WithEncodeOrWithDecodeDsl<T, EncodeBlock<T>, DecodeBlock<T>, BuildCodecBlock<T>>,
        CodecWithConfigDsl<T>

/**
 * The contract of what can be done after defineCodecFilter function
 */
interface DefineCodecFilterDsl<T : Any> :
        WithEncodeOrWithDecodeDsl<T, FilterEncodeBlock<T>, FilterDecodeBlock<T>, BuildFilteredCodecBlock<T>>,
        CodecFilterWithConfigDsl<T>

/**
 * The contract providing the option to define an encode or a decode block
 */
interface WithEncodeOrWithDecodeDsl<T : Any, E, D, R> :
        ThatEncodesByDsl<T, E, AndDecodesByDsl<T, D, R>>,
        ThatDecodesByDsl<T, D, AndEncodesByDsl<T, E, R>>

/**
 * The contract of defining a configuration for a codec
 */
interface CodecWithConfigDsl<T : Any> {
    infix fun <C : Any> withConfig(supplyDefaultConfigBlock: SupplyDefaultConfigBlock<C>):
            ConfigurableCodecDsl<T, C>
}

typealias ConfigurableCodecDsl<T, C> =
        WithEncodeOrWithDecodeDsl<T,
                EncodeWithConfigBlock<T, C>,
                DecodeWithConfigBlock<T, C>,
                BuildCodecWithOverrideBlock<T, C>>

/**
 * The contract of defining a configuration for a codec filter
 */
interface CodecFilterWithConfigDsl<T : Any> {
    infix fun <C : Any> withConfig(supplyDefaultConfigBlock: SupplyDefaultConfigBlock<C>):
            ConfigurableCodecFilterDsl<T, C>
}

typealias ConfigurableCodecFilterDsl<T, C> =
        WithEncodeOrWithDecodeDsl<T,
                FilterEncodeWithConfigBlock<T, C>,
                FilterDecodeWithConfigBlock<T, C>,
                BuildFilteredCodecWithConfigBlock<T, C>>

/**
 * The contract of defining an encode block
 */
interface ThatEncodesByDsl<T : Any, P, R> {
    infix fun thatEncodesBy(encodeBlock: P): R
}

/**
 * The contract of defining a decode block
 */
interface ThatDecodesByDsl<T : Any, P, R> {
    infix fun thatDecodesBy(decodeBlock: P): R
}

/**
 * The contract of defining an encode block
 */
interface AndEncodesByDsl<T : Any, P, R> {
    infix fun andEncodesBy(encodeBlock: P): R
}

/**
 * The contract of defining a decode block
 */
interface AndDecodesByDsl<T : Any, P, R> {
    infix fun andDecodesBy(decodeBlock: P): R
}

/**
 * The contract of what can be done after defineSegmentProperty function
 */
interface DefineSegmentPropertyDsl<T : Any> {
    infix fun using(buildCodecBlock: BuildCodecBlock<T>): FilterDsl<T>
    infix fun <C : Any> using(configurableCodecFactory: BuildCodecWithOverrideBlock<T, C>): FilterOrConfigDsl<T, C>
}

/**
 * The contract of providing an option to define config override together with filtering
 */
interface FilterOrConfigDsl<T : Any, C : Any> : FilterDsl<T> {
    infix fun withOverride(overrideConfigBlock: OverrideConfigBlock<C>): FilterDsl<T>
}

/**
 * The contract of providing the option to filter with or without a configuration
 */
interface FilterDsl<T : Any> {
    infix fun filterWith(codecFilterFactory: BuildFilteredCodecBlock<T>): FilterDsl<T>
    infix fun <C : Any> filterWith(configurableCodecFilterFactory: BuildFilteredCodecWithConfigBlock<T, C>):
            FilterOrConfigDsl<T, C>
}

/**
 * The contract of what can be done after defineSegmentCodec function
 */
interface DefineSegmentCodecDsl<T : Segment> :
        WithEncodeOrWithDecodeDsl<T, EncodeSegmentBlock<T>, DecodeSegmentBlock<T>, BuildCodecBlock<T>>

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
fun <T : Any> defineSegmentProperty(): DefineSegmentPropertyDsl<T> = S()

/**
 * The entrypoint function of defining a segment codec
 */
fun <T : Segment> defineSegmentCodec(segmentClass: KClass<T>): DefineSegmentCodecDsl<T> = SC(segmentClass)

/**
 * A convenience idiomatic kotlin function delegating to defineSegmentCodec function
 */
inline fun <reified T : Segment> defineSegmentCodec(): DefineSegmentCodecDsl<T> = defineSegmentCodec(T::class)