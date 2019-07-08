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
import kotlin.reflect.full.createInstance

class CodecContext internal constructor(private val configs: Map<KClass<*>, Any>) {
    fun <C : Any> get(configClass: KClass<C>) = (configs[configClass]
            ?: throw ConfigurationException("Configuration class ${configClass.qualifiedName} not found")) as C
}

class CodecContextTemplate internal constructor(private val configTemplates: Set<ConfigTemplate<*>>) {
    internal fun createNew() = CodecContext(configTemplates.map { it.createNew() }.map { it::class to it }.toMap())
}

typealias ConfigSpec<T> = T.() -> Unit

internal class ConfigTemplate<T : Any>(private val configClass: KClass<T>, private val configSpec: ConfigSpec<T>) {
    fun createNew(): T = try {
        configClass.createInstance().apply(configSpec)
    } catch (e: IllegalArgumentException) {
        throw ConfigurationException("Invalid configuration class ${configClass.qualifiedName}", e)
    }

    override fun equals(other: Any?) = when (other) {
        is ConfigTemplate<*> -> configClass == other.configClass
        else -> false
    }

    override fun hashCode() = configClass.hashCode()
}

class CodecContextTemplateBuilder internal constructor() {
    private val templates = mutableSetOf<ConfigTemplate<*>>()

    fun <C : Any> with(configClass: KClass<C>, configSpec: ConfigSpec<C> = {}) {
        if (!templates.add(ConfigTemplate(configClass, configSpec))) {
            throw ConfigurationException("Configuration for ${configClass.qualifiedName} already defined")
        }
    }

    internal fun build() = CodecContextTemplate(templates)
}

fun codecContextTemplate(spec: CodecContextTemplateBuilder.() -> Unit) =
        CodecContextTemplateBuilder().apply(spec).build()
