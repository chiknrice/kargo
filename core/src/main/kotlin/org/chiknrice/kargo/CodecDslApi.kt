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

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * The contract of what can be done after defineCodec function
 */
interface DefineCodecDsl<T : Any> :
        ThatEncodesByDsl<T, EncodeSpec<T>, DecodeSpec<T>, CodecDefinition<T>>,
        CodecWithConfigDsl<T>

/**
 * The contract of what can be done after defineCodecFilter function
 */
interface DefineCodecFilterDsl<T : Any> :
        ThatEncodesByDsl<T, FilterEncodeSpec<T>, FilterDecodeSpec<T>, FilterDefinition<T>>,
        CodecFilterWithConfigDsl<T>

/**
 * The contract of defining a configuration for a codec
 */
interface CodecWithConfigDsl<T : Any> {
    infix fun <C : Any> withConfig(configClass: KClass<C>):
            ConfigurableCodecDsl<T, C>
}

/**
 * The contract of optionally overriding configuration of type C of a codec for type T
 */
interface OverridableCodecConfigDsl<T : Any, C : Any> : CodecDefinition<T> {
    fun withOverrides(overrides: ConfigSpec<C>): OverridableCodecConfigDsl<T, C>
}

typealias ConfigurableCodecDsl<T, C> =
        ThatEncodesByDsl<T,
                ConfigurableEncodeSpec<T, C>,
                ConfigurableDecodeSpec<T, C>,
                OverridableCodecConfigDsl<T, C>>

/**
 * The contract of defining a configuration for a codec filter
 */
interface CodecFilterWithConfigDsl<T : Any> {
    infix fun <C : Any> withConfig(configClass: KClass<C>):
            ConfigurableCodecFilterDsl<T, C>
}

/**
 * The contract of optionally overriding configuration of type C of a filter for type T
 */
interface OverridableFilterConfigDsl<T : Any, C : Any> : FilterDefinition<T> {
    fun withOverrides(overrides: ConfigSpec<C>): OverridableFilterConfigDsl<T, C>
}

typealias ConfigurableCodecFilterDsl<T, C> =
        ThatEncodesByDsl<T,
                ConfigurableFilterEncodeSpec<T, C>,
                ConfigurableFilterDecodeSpec<T, C>,
                OverridableFilterConfigDsl<T, C>>

/**
 * The contract of defining an encode specification
 */
interface ThatEncodesByDsl<T : Any, E, D, R> {
    infix fun thatEncodesBy(encodeSpec: E): AndDecodesByDsl<T, D, R>
}

/**
 * The contract of defining a decode specification
 */
interface AndDecodesByDsl<T : Any, D, R> {
    infix fun andDecodesBy(decodeSpec: D): R
}

/**
 * The contract of what can be done after defineSegmentProperty function
 */
interface DefineSegmentPropertyDsl<T : Any> {
    infix fun using(codecDefinition: CodecDefinition<T>): WrappedWithDsl<T>
}

/**
 * The contract of providing a property delegate
 */
interface DelegateProvider<T : Any> {
    operator fun provideDelegate(thisRef: Segment, property: KProperty<*>): ReadWriteProperty<Segment, T?>
}

/**
 * The contract of providing the option to filter with or without a configuration
 */
interface WrappedWithDsl<T : Any> : DelegateProvider<T> {
    infix fun wrappedWith(filterDefinition: FilterDefinition<T>): ThenWithDsl<T>
}

/**
 * The contract of providing the option to further filter with or without a configuration
 */
interface ThenWithDsl<T : Any> : DelegateProvider<T> {
    infix fun thenWith(filterDefinition: FilterDefinition<T>): ThenWithDsl<T>
}

/**
 * The contract of what can be done after defineSegmentCodec function
 */
interface DefineSegmentCodecDsl<T : Segment> :
        ThatEncodesByDsl<T, EncodeSegmentSpec<T>, DecodeSegmentSpec<T>, CodecDefinition<T>>

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
fun <T : Any> defineProperty(): DefineSegmentPropertyDsl<T> = S()

/**
 * The entrypoint function of defining a segment codec
 */
fun <T : Segment> defineSegmentCodec(segmentClass: KClass<T>): DefineSegmentCodecDsl<T> = SC(segmentClass)

/**
 * A convenience idiomatic kotlin function delegating to defineSegmentCodec function
 */
inline fun <reified T : Segment> defineSegmentCodec(): DefineSegmentCodecDsl<T> = defineSegmentCodec(T::class)