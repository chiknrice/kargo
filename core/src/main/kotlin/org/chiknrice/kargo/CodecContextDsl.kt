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

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class CodecContext internal constructor(private val configs: Map<KClass<*>, Any>) {
    fun <C : Any> get(configClass: KClass<C>) = (configs[configClass]
            ?: throw ConfigurationException("Configuration class ${configClass.qualifiedName} not found")) as C
}

class CodecContextTemplate internal constructor(private val configTemplates: Collection<ConfigTemplate<*>>) {
    internal fun createNew() = mutableMapOf<KClass<*>, Any>().also { map ->
        configTemplates.forEach { template -> template.createNew().also { config -> map[config::class] = config } }
    }.let { CodecContext(it) }
}

typealias ConfigSpec<T> = T.() -> Unit

internal class ConfigTemplate<T : Any>(private val configClass: KClass<T>, private val configSpec: ConfigSpec<T>) {
    fun createNew(): T = try {
        configClass.createInstance().apply(configSpec)
    } catch (e: Exception) {
        throw ConfigurationException("Invalid configuration class ${configClass.qualifiedName}", e)
    }
}

class CodecContextTemplateBuilder internal constructor() {
    private val map = mutableMapOf<KClass<*>, ConfigTemplate<*>>()

    fun <C : Any> with(configClass: KClass<C>, configSpec: ConfigSpec<C> = {}) {
        if (map.containsKey(configClass)) {
            throw ConfigurationException("Configuration for ${configClass.qualifiedName} already defined")
        } else {
            map[configClass] = ConfigTemplate(configClass, configSpec)
        }
    }

    internal fun build() = CodecContextTemplate(map.values)
}

fun codecContextTemplate(spec: CodecContextTemplateBuilder.() -> Unit) =
        CodecContextTemplateBuilder().apply(spec).build()