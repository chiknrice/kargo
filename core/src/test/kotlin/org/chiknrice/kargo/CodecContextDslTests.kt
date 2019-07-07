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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

data class ValidConfig(var intProperty: Int = 0)
data class AnotherValidConfig(var stringProperty: String = "")
data class AnInvalidConfig(var stringProperty: String)

class CodecContextDslTests {

    @Test
    fun `Creating an empty CodecContextTemplate is allowed`() {
        var template = codecContextTemplate { }
        assertThat(template).isInstanceOf(CodecContextTemplate::class.java)
    }

    @Test
    fun `Creating a CodecContextTemplate with ValidConfig without a config spec is allowed and result in defaults`() {
        val codecContextTemplate = codecContextTemplate {
            with(ValidConfig::class)
        }
        val codecContext = codecContextTemplate.createNew()
        assertThat(codecContext.get(ValidConfig::class).intProperty).isEqualTo(0)
    }

    @Test
    fun `Creating a CodecContextTemplate with ValidConfig without a config spec results in an object with defaults`() {
        val codecContextTemplate = codecContextTemplate {
            with(ValidConfig::class)
        }
        val codecContext = codecContextTemplate.createNew()
        assertThat(codecContext.get(ValidConfig::class).intProperty).isEqualTo(0)
    }

    @Test
    fun `Creating a CodecContextTemplate with AnInvalidConfig class would result in ConfigurationException`() {
        assertThatThrownBy {
            codecContextTemplate {
                with(AnInvalidConfig::class)
            }.createNew()
        }.isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Invalid configuration class org.chiknrice.kargo.AnInvalidConfig")
    }

    @Test
    fun `CodecContext created by a CodecContextTemplate with class ValidConfig should contain an object of type ValidConfig`() {
        val template = codecContextTemplate {
            with(ValidConfig::class) {
                intProperty = 5
            }
        }
        val codecContext = template.createNew()
        assertThat(codecContext.get(ValidConfig::class)).isEqualTo(ValidConfig(5))
    }

    @Test
    fun `Defining the same config class more than once would result in exception`() {
        assertThatThrownBy {
            codecContextTemplate {
                with(ValidConfig::class)
                with(ValidConfig::class)
            }
        }.isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration for org.chiknrice.kargo.ValidConfig already defined")

    }

    @Test
    fun `Creating another CodecConfig from the same CodecContextTemplate would result in different object tree`() {
        val codecContextTemplate = codecContextTemplate {
            with(ValidConfig::class)
        }
        val codecContext1 = codecContextTemplate.createNew()
        val codecContext2 = codecContextTemplate.createNew()
        assertThat(codecContext2).isNotEqualTo(codecContext1)
        // since ValidConfig is a data class
        val validConfig2 = codecContext2.get(ValidConfig::class)
        val validConfig1 = codecContext1.get(ValidConfig::class)
        assertThat(validConfig2).isEqualTo(validConfig1)
        // but are 2 different objects
        assertThat(validConfig2).isNotSameAs(validConfig1)
        // changing one object proves this
        validConfig2.intProperty++
        assertThat(validConfig2).isNotEqualTo(validConfig1)
    }

    @Test
    fun `Getting a config which was not configured will throw an exception`() {
        assertThatThrownBy {
            codecContextTemplate {
                with(ValidConfig::class)
            }.createNew().get(AnotherValidConfig::class)
        }.isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Configuration class org.chiknrice.kargo.AnotherValidConfig not found")
    }

}