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
        ThatEncodesByDsl<T, EncodeBlock<T>, DecodeBlock<T>, BuildCodecBlock<T>>,
        CodecWithConfigDsl<T>

/**
 * The contract of what can be done after defineCodecFilter function
 */
interface DefineCodecFilterDsl<T : Any> :
        ThatEncodesByDsl<T, FilterEncodeBlock<T>, FilterDecodeBlock<T>, WrapCodecWithFilterBlock<T>>,
        CodecFilterWithConfigDsl<T>

/**
 * The contract of defining a configuration for a codec
 */
interface CodecWithConfigDsl<T : Any> {
    infix fun <C : Any> withConfig(configClass: KClass<C>):
            ConfigurableCodecDsl<T, C>
}

typealias ConfigurableCodecDsl<T, C> =
        ThatEncodesByDsl<T,
                EncodeWithConfigBlock<T, C>,
                DecodeWithConfigBlock<T, C>,
                BuildConfigurableCodecBlock<T, C>>

/**
 * The contract of defining a configuration for a codec filter
 */
interface CodecFilterWithConfigDsl<T : Any> {
    infix fun <C : Any> withConfig(configClass: KClass<C>):
            ConfigurableCodecFilterDsl<T, C>
}

typealias ConfigurableCodecFilterDsl<T, C> =
        ThatEncodesByDsl<T,
                FilterEncodeWithConfigBlock<T, C>,
                FilterDecodeWithConfigBlock<T, C>,
                WrapCodecWithConfigurableFilterBlock<T, C>>

/**
 * The contract of defining an encode block
 */
interface ThatEncodesByDsl<T : Any, E, D, R> {
    infix fun thatEncodesBy(encodeBlock: E): AndDecodesByDsl<T, D, R>
}

/**
 * The contract of defining a decode block
 */
interface AndDecodesByDsl<T : Any, D, R> {
    infix fun andDecodesBy(decodeBlock: D): R
}

/**
 * The contract of what can be done after defineSegmentProperty function
 */
interface DefineSegmentPropertyDsl<T : Any> {
    infix fun using(buildCodecBlock: BuildCodecBlock<T>): WrappedWithDsl<T>
    infix fun <C : Any> using(buildConfigurableCodecBlock: BuildConfigurableCodecBlock<T, C>):
            WrappedWithOrConfigWithDsl<T, C>
}

/**
 * The contract of providing an option to define config override together with filtering
 */
interface WrappedWithOrConfigWithDsl<T : Any, C : Any> : WrappedWithDsl<T> {
    infix fun withConfig(overrideConfigBlock: OverrideConfigBlock<C>): WrappedWithDsl<T>
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
    infix fun wrappedWith(wrapCodecWithFilterBlock: WrapCodecWithFilterBlock<T>): ThenWithDsl<T>
    infix fun <C : Any> wrappedWith(wrapCodecWithConfigurableFilterBlock: WrapCodecWithConfigurableFilterBlock<T, C>):
            ThenWithOrWithConfigDsl<T, C>
}

/**
 * The contract of providing the option to define config override together with further filtering
 */
interface ThenWithOrWithConfigDsl<T : Any, C : Any> : ThenWithDsl<T> {
    infix fun withConfig(overrideConfigBlock: OverrideConfigBlock<C>): ThenWithDsl<T>
}

/**
 * The contract of providing the option to further filter with or without a configuration
 */
interface ThenWithDsl<T : Any> : DelegateProvider<T> {
    infix fun thenWith(wrapCodecWithFilterBlock: WrapCodecWithFilterBlock<T>): ThenWithDsl<T>
    infix fun <C : Any> thenWith(wrapCodecWithConfigurableFilterBlock: WrapCodecWithConfigurableFilterBlock<T, C>):
            ThenWithOrWithConfigDsl<T, C>
}

/**
 * The contract of what can be done after defineSegmentCodec function
 */
interface DefineSegmentCodecDsl<T : Segment> :
        ThatEncodesByDsl<T, EncodeSegmentBlock<T>, DecodeSegmentBlock<T>, BuildCodecBlock<T>>

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