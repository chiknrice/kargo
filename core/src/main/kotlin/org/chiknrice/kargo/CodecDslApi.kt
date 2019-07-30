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

@DslMarker
annotation class KargoDsl

@KargoDsl
interface DefinitionDsl<T : Any, E : Any, D : Any> {
    fun encode(encodeSpec: E)
    fun decode(decodeSpec: D)
}

@KargoDsl
interface ConfigurableDefinitionDsl<T : Any, C : Any, E : Any, D : Any> : DefinitionDsl<T, E, D> {
    fun config(configSpec: ConfigSpec<C>)
}

typealias CodecDefinitionDsl<T> = DefinitionDsl<T, EncodeSpec<T>, DecodeSpec<T>>
typealias ConfigurableCodecDefinitionDsl<T, C> = ConfigurableDefinitionDsl<T, C, ConfigurableEncodeSpec<T, C>,
        ConfigurableDecodeSpec<T, C>>

typealias FilterDefinitionDsl<T> = DefinitionDsl<T, FilterEncodeSpec<T>, FilterDecodeSpec<T>>
typealias ConfigurableFilterDefinitionDsl<T, C> = ConfigurableDefinitionDsl<T, C, ConfigurableFilterEncodeSpec<T, C>,
        ConfigurableFilterDecodeSpec<T, C>>

typealias SegmentCodecDefinitionDsl<T> = DefinitionDsl<T, EncodeSegmentSpec<T>, DecodeSegmentSpec<T>>

@KargoDsl
abstract class Definition {
    fun <T : Any> codec(codecSpec: CodecDefinitionDsl<T>.() -> Unit) =
            CodecDefinitionDslImpl<T>().apply(codecSpec).build()

    fun <T : Any, C : Any> codec(configClass: KClass<C>, codecSpec: ConfigurableCodecDefinitionDsl<T, C>.() -> Unit) =
            ConfigurableCodecDefinitionDslImpl<T, C>(configClass).apply(codecSpec).build()

    inline fun <T : Any, reified C : Any> codec(noinline codecSpec: ConfigurableCodecDefinitionDsl<T, C>.() -> Unit) =
            codec(C::class, codecSpec)

    fun <T : Any> filter(filterSpec: FilterDefinitionDsl<T>.() -> Unit) =
            FilterDefinitionDslImpl<T>().apply(filterSpec).build()

    fun <T : Any, C : Any> filter(configClass: KClass<C>,
                                  filterSpec: ConfigurableFilterDefinitionDsl<T, C>.() -> Unit) =
            ConfigurableFilterDefinitionDslImpl<T, C>(configClass).apply(filterSpec).build()

    inline fun <T : Any, reified C : Any> filter(
            noinline filterSpec: ConfigurableFilterDefinitionDsl<T, C>.() -> Unit) =
            filter(C::class, filterSpec)

    fun <T : Segment> segmentCodec(segmentClass: KClass<T>, codecSpec: SegmentCodecDefinitionDsl<T>.() -> Unit) =
            SegmentCodecDefinitionDslImpl(segmentClass).apply(codecSpec).build()

    inline fun <reified T : Segment> segmentCodec(noinline codecSpec: SegmentCodecDefinitionDsl<T>.() -> Unit) =
            segmentCodec(T::class, codecSpec)
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
 * The entrypoint function of defining a segment property
 */
fun <T : Any> defineProperty(): DefineSegmentPropertyDsl<T> = S()
