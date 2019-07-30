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
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.*

internal fun <C : Any> KClass<C>.createConfigInstance(overridesChain: List<ConfigSpec<C>>) =
        this.createInstance().also { config ->
            overridesChain.forEach {
                config.apply(it)
            }
        }

internal class Validator {
    private val deferredValidations = mutableListOf<(MutableList<String>) -> Unit>()
    private val errors = mutableListOf<String>()

    fun addValidation(validation: (MutableList<String>) -> Unit) {
        deferredValidations.add(validation)
    }

    fun addError(message: String) {
        errors.add(message)
    }

    fun validate() {
        deferredValidations.forEach { it(errors) }
        if (errors.isNotEmpty()) {
            val message = when (errors.size) {
                1 -> errors.first()
                else -> errors.joinToString(prefix = "Errors:${System.lineSeparator()}",
                        separator = System.lineSeparator()) { " - $it" }
            }
            throw CodecDefinitionException(message)
        }
    }
}

internal abstract class DefinitionDslImpl<T : Any, E : Any, D : Any, R : Any> : DefinitionDsl<T, E, D> {
    lateinit var encodeSpec: E
    lateinit var decodeSpec: D
    val validator = Validator()

    init {
        validator.addValidation { errors ->
            if (!::encodeSpec.isInitialized) errors.add("Encode spec is required")
            if (!::decodeSpec.isInitialized) errors.add("Decode spec is required")
        }
    }

    final override fun encode(encodeSpec: E) {
        if (this::encodeSpec.isInitialized) validator.addError("Encode spec declared multiple times")
        this.encodeSpec = encodeSpec
    }

    final override fun decode(decodeSpec: D) {
        if (this::decodeSpec.isInitialized) validator.addError("Decode spec declared multiple times")
        this.decodeSpec = decodeSpec
    }

    fun build(): R {
        validator.validate()
        return buildDefinition()
    }

    protected abstract fun buildDefinition(): R
}

internal abstract class ConfigurableDefinitionDslImpl<T : Any, C : Any, E : Any, D : Any, R : Any>(
        val configClass: KClass<C>) : ConfigurableDefinitionDsl<T, C, E, D>, DefinitionDslImpl<T, E, D, R>() {
    init {
        validator.addValidation { errors ->
            try {
                configClass.createInstance()
            } catch (e: IllegalArgumentException) {
                if (e.message != null && e.message!!.startsWith("Class should have a single no-arg constructor"))
                    errors.add("Configuration class ${configClass.simpleName} does not have a no-arg constructor")
            }
        }
    }

    var configSpecs: List<ConfigSpec<C>> = emptyList()

    final override fun config(configSpec: ConfigSpec<C>) {
        if (configSpecs.isNotEmpty()) validator.addError("Config spec declared multiple times")
        configSpecs = listOf(configSpec)
    }
}

internal class CodecDefinitionDslImpl<T : Any> :
        DefinitionDslImpl<T, EncodeSpec<T>, DecodeSpec<T>, CodecDefinition<T>>() {
    override fun buildDefinition() = object : CodecDefinition<T> {
        override fun buildCodec() = object : Codec<T> {
            override fun encode(value: T, buffer: ByteBuffer) = encodeSpec(value, buffer)
            override fun decode(buffer: ByteBuffer) = decodeSpec(buffer)
        }
    }
}

internal class ConfigurableCodecDefinitionDslImpl<T : Any, C : Any>(configClass: KClass<C>) :
        ConfigurableDefinitionDslImpl<T, C,
                ConfigurableEncodeSpec<T, C>,
                ConfigurableDecodeSpec<T, C>,
                ConfigurableCodecDefinition<T, C>>(configClass),
        ConfigurableCodecDefinitionDsl<T, C> {
    override fun buildDefinition(): ConfigurableCodecDefinition<T, C> =
            ConfigurableCodecDefinitionImpl(configClass, configSpecs, encodeSpec, decodeSpec)

    class ConfigurableCodecDefinitionImpl<T : Any, C : Any>(private val configClass: KClass<C>,
                                                            private val configSpecs: List<ConfigSpec<C>>,
                                                            private val encodeSpec: ConfigurableEncodeSpec<T, C>,
                                                            private val decodeSpec: ConfigurableDecodeSpec<T, C>) :
            ConfigurableCodecDefinition<T, C> {
        override fun withOverrides(overrides: ConfigSpec<C>) =
                ConfigurableCodecDefinitionImpl(configClass, configSpecs.plusElement(overrides), encodeSpec, decodeSpec)

        override fun buildCodec() = configClass.createConfigInstance(configSpecs).let {
            object : Codec<T> {
                override fun encode(value: T, buffer: ByteBuffer) = encodeSpec(value, buffer, it)
                override fun decode(buffer: ByteBuffer) = decodeSpec(buffer, it)
            }
        }
    }
}

internal class FilterDefinitionDslImpl<T : Any> :
        DefinitionDslImpl<T, FilterEncodeSpec<T>, FilterDecodeSpec<T>, FilterDefinition<T>>() {
    override fun buildDefinition() = object : FilterDefinition<T> {
        override fun wrapCodec(chain: Codec<T>) = object : Codec<T> {
            override fun encode(value: T, buffer: ByteBuffer) = encodeSpec(value, buffer, chain)
            override fun decode(buffer: ByteBuffer) = decodeSpec(buffer, chain)
        }
    }
}

internal class ConfigurableFilterDefinitionDslImpl<T : Any, C : Any>(configClass: KClass<C>) :
        ConfigurableDefinitionDslImpl<T, C,
                ConfigurableFilterEncodeSpec<T, C>,
                ConfigurableFilterDecodeSpec<T, C>,
                ConfigurableFilterDefinition<T, C>>(configClass) {
    override fun buildDefinition(): ConfigurableFilterDefinition<T, C> =
            ConfigurableFilterDefinitionImpl(configClass, configSpecs, encodeSpec, decodeSpec)

    class ConfigurableFilterDefinitionImpl<T : Any, C : Any>(private val configClass: KClass<C>,
                                                             private val configSpecs: List<ConfigSpec<C>>,
                                                             private val encodeSpec: ConfigurableFilterEncodeSpec<T, C>,
                                                             private val decodeSpec: ConfigurableFilterDecodeSpec<T, C>) :
            ConfigurableFilterDefinition<T, C> {
        override fun withOverrides(overrides: ConfigSpec<C>) =
                ConfigurableFilterDefinitionImpl(configClass, configSpecs.plusElement(overrides), encodeSpec,
                        decodeSpec)

        override fun wrapCodec(chain: Codec<T>) = configClass.createConfigInstance(configSpecs).let {
            object : Codec<T> {
                override fun encode(value: T, buffer: ByteBuffer) = encodeSpec(value, buffer, it, chain)
                override fun decode(buffer: ByteBuffer) = decodeSpec(buffer, it, chain)
            }
        }
    }
}

internal class SegmentCodecDefinitionDslImpl<T : Segment>(private val segmentClass: KClass<T>) :
        DefinitionDslImpl<T, EncodeSegmentSpec<T>, DecodeSegmentSpec<T>, CodecDefinition<T>>() {
    init {
        validator.addValidation { errors ->
            try {
                val instance = segmentClass.createInstance()
                if (instance.properties.isEmpty()) {
                    errors.add("Segment class [${segmentClass.simpleName}] is required to have a segment property")
                }
            } catch (e: IllegalArgumentException) {
                if (e.message != null && e.message!!.startsWith("Class should have a single no-arg constructor"))
                    errors.add("Segment class ${segmentClass.simpleName} does not have a no-arg constructor")
            }
            segmentClass.memberProperties.filter {
                it.returnType.isSubtypeOf(DelegateProvider::class.createType(
                        listOf(KTypeProjection.invariant(Any::class.starProjectedType))))
            }.map { it.name }.toList().also {
                if (it.isNotEmpty()) errors.add("Properties are incorrectly assigned: $it")
            }
        }
    }

    override fun buildDefinition() = object : CodecDefinition<T> {
        override fun buildCodec() = object : Codec<T> {
            override fun encode(value: T, buffer: ByteBuffer) =
                    encodeSpec(value, SegmentProperties(value.properties), buffer)

            override fun decode(buffer: ByteBuffer) = segmentClass.createInstance().apply {
                decodeSpec(SegmentProperties(this.properties), buffer, this)
                if (buffer.hasRemaining()) throw CodecException("Buffer still has remaining bytes")
            }
        }

    }
}

/**
 * PropertyContext defines the coordinates of a particular property (e.g. in which class the property belongs to)
 */
internal data class PropertyContext(val kClass: KClass<*>, val kProperty: KProperty<*>)

internal class S<T : Any> : DefineSegmentPropertyDsl<T> {
    override infix fun using(codecDefinition: CodecDefinition<T>) = SC(codecDefinition)

    class SC<T : Any>(private val codecDefinition: CodecDefinition<T>) : SegmentPropertyProvider<T>() {
        override fun buildCodec() = codecDefinition.buildCodec()
    }

    class SF<T : Any>(private val chain: Codec<T>, private val filterDefinition: FilterDefinition<T>) :
            SegmentPropertyProvider<T>() {
        override fun buildCodec() = filterDefinition.wrapCodec(chain)
    }

    /**
     * A common class which provides a SegmentProperty in any scenario which is possible to terminate a
     * defineSegmentProperty statement
     */
    abstract class SegmentPropertyProvider<T : Any> : WrappedWithDsl<T>, ThenWithDsl<T> {

        override infix fun wrappedWith(filterDefinition: FilterDefinition<T>) = SF(buildCodec(), filterDefinition)

        override infix fun thenWith(filterDefinition: FilterDefinition<T>) = SF(buildCodec(), filterDefinition)

        abstract fun buildCodec(): Codec<T>

        override operator fun provideDelegate(thisRef: Segment,
                                              property: KProperty<*>): ReadWriteProperty<Segment, T?> {
            // property codecs scope is only one per class-property - while segment property is one per segment instance
            val segmentClass = thisRef::class
            val propertyContext = PropertyContext(segmentClass, property)
            if (!propertyCodecs.containsKey(propertyContext)) {
                synchronized(segmentClass) {
                    if (!propertyCodecs.containsKey(propertyContext)) {
                        propertyCodecs[propertyContext] = buildCodec()
                    }
                }
            }
            return SegmentProperty(propertyContext, propertyCodecs[propertyContext] as Codec<T>).also {
                thisRef.properties.add(it)
            }
        }
    }

    companion object {
        val propertyCodecs = mutableMapOf<PropertyContext, Codec<*>>()
    }

}

