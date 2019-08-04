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
import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.*

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
        val configClass: KClass<C>, val configSpec: ConfigSpec<C>) : DefinitionDslImpl<T, E, D, R>() {
    init {
        validator.addValidation { errors ->
            try {
                configClass.createInstance()
            } catch (e: Exception) {
                if (e.message != null && e.message!!.contains("should have a single no-arg constructor"))
                    errors.add("Configuration class ${configClass.simpleName} does not have a no-arg constructor")
                else throw e
            }
        }
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

internal class ConfigurableCodecDefinitionDslImpl<T : Any, C : Any>(configClass: KClass<C>, configSpec: ConfigSpec<C>) :
        ConfigurableDefinitionDslImpl<T, C,
                ConfigurableEncodeSpec<T, C>,
                ConfigurableDecodeSpec<T, C>,
                CodecDefinition<T>>(configClass, configSpec),
        ConfigurableCodecDefinitionDsl<T, C> {
    override fun buildDefinition() = object : CodecDefinition<T> {
        override fun buildCodec(): Codec<T> {
            val config = configClass.createInstance().apply(configSpec)
            return object : Codec<T> {
                override fun encode(value: T, buffer: ByteBuffer) = encodeSpec(value, buffer, config)
                override fun decode(buffer: ByteBuffer) = decodeSpec(buffer, config)
            }
        }

    }
}

internal class FilterDefinitionDslImpl<T : Any> :
        DefinitionDslImpl<T, FilterEncodeSpec<T>, FilterDecodeSpec<T>, FilterDefinition<T>>() {
    override fun buildDefinition() = object : FilterDefinition<T> {
        override fun wrapCodec(codec: Codec<T>) = object : Codec<T> {
            override fun encode(value: T, buffer: ByteBuffer) = encodeSpec(value, buffer, codec)
            override fun decode(buffer: ByteBuffer) = decodeSpec(buffer, codec)
        }
    }
}

internal class ConfigurableFilterDefinitionDslImpl<T : Any, C : Any>(configClass: KClass<C>,
                                                                     configSpec: ConfigSpec<C>) :
        ConfigurableDefinitionDslImpl<T, C,
                ConfigurableFilterEncodeSpec<T, C>,
                ConfigurableFilterDecodeSpec<T, C>,
                FilterDefinition<T>>(configClass, configSpec) {
    override fun buildDefinition() = object : FilterDefinition<T> {
        override fun wrapCodec(codec: Codec<T>): Codec<T> {
            val config = configClass.createInstance().apply(configSpec)
            return object : Codec<T> {
                override fun encode(value: T, buffer: ByteBuffer) = encodeSpec(value, buffer, config, codec)
                override fun decode(buffer: ByteBuffer) = decodeSpec(buffer, config, codec)
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
            } catch (e: Exception) {
                if (e.message != null && e.message!!.contains("should have a single no-arg constructor"))
                    errors.add("Segment class ${segmentClass.simpleName} does not have a no-arg constructor")
                else throw e
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

internal class FiltersCodecDslImpl<T : Any>(val filterDefinitions: List<FilterDefinition<T>>) : FiltersCodecDsl<T> {
    override fun filters(codecDefinition: CodecDefinition<T>) = object : CodecDefinition<T> {
        override fun buildCodec(): Codec<T> {
            return filterDefinitions.foldRight(codecDefinition.buildCodec()) { filterDefinition, codec ->
                filterDefinition.wrapCodec(codec)
            }
        }
    }
}

