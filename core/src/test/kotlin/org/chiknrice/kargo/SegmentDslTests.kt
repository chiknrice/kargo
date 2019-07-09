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

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.ScriptException

private data class StringConfig(var length: Int)

val mockCodec = mockk<Codec<String>>()
val mockCodecContext = mockk<CodecContext>()
val mockCodecContextTemplate = mockk<CodecContextTemplate>()

class SegmentDslTests {

    private val stringConfig = StringConfig(5)

    @BeforeEach
    fun setupMocks() {
        clearAllMocks()
        every { mockCodecContextTemplate.createNew() } returns mockCodecContext
        every { mockCodecContext.get(StringConfig::class) } returns stringConfig
    }

    @Test
    fun `Segments properties can be val or var and the value defaults to null`() {
        class ExampleSegment : Segment() {
            val readOnlyElement by segment(mockCodec, mockCodecContextTemplate)
            var readWriteElement by segment(mockCodec, mockCodecContextTemplate)
        }

        val exampleSegment = ExampleSegment()
        assertThat(exampleSegment.readOnlyElement).isNull()

        exampleSegment.readWriteElement = "test"
        assertThat(exampleSegment.readWriteElement).isEqualTo("test")

    }

    @Test
    fun `Segment classes only creates segment properties via segment dsl`() {
        class ExampleSegment : Segment() {
            val element1 by segment(mockCodec, mockCodecContextTemplate)
            var aNormalProperty: String = ""
        }

        val exampleSegment = ExampleSegment()
        assertThat(exampleSegment.properties.size).isEqualTo(1)
        assertThat(exampleSegment.properties.keys.first().name).isEqualTo("element1")
    }

    @Test
    fun `The order of segment properties maintain the order which they are defined in the class`() {
        class ExampleSegment : Segment() {
            val element1 by segment(mockCodec, mockCodecContextTemplate)
            val element2 by segment(mockCodec, mockCodecContextTemplate)
            val element3 by segment(mockCodec, mockCodecContextTemplate)
        }

        val exampleSegment = ExampleSegment()
        assertThat(exampleSegment.properties.size).isEqualTo(3)
        assertThat(exampleSegment.properties.keys.map { it.name }).isEqualTo(listOf("element1", "element2", "element3"))
    }

    @Test
    fun `Configuration overrides take precedence over configuration set during codec context template creation`() {

    }

    @Test
    fun `Property encode and decode methods delegate to codec with the configured codec context`() {

    }

    @Test
    fun `Properties created with same codec and codec context template will each have a different codec context`() {

    }

    @Test
    fun `Property codecs can be shared and should have no state`() {

    }

    @Test
    fun `Property encode method delegate to codec passing the current value`() {

    }

    @Test
    fun `Property decode method delegates to codec and sets the decoded value as the property's current value`() {

    }

}

class SegmentCompileTimeTests {

    private fun scriptEngine() = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223JvmLocalScriptEngine

    @Test
    fun `Creating segment properties in a class that doesn't inherit from Segment will result in compile error`() {
        val scriptEngine = scriptEngine()
        val bindings = scriptEngine.createBindings()
        bindings["mockCodec"] = mockCodec
        bindings["mockCodecContextTemplate"] = mockCodecContextTemplate
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE)

        with(scriptEngine) {
            compile("""
                    import org.chiknrice.kargo.*
                    
                    class ExampleNonSegment : Segment() {
                        val aSegmentProperty by segment(mockCodec, mockCodecContextTemplate)
                    }
                """.trimIndent())
        }
        assertThatThrownBy {
            with(scriptEngine) {
                compile("""
                    import org.chiknrice.kargo.*
                    
                    class ExampleNonSegment {
                        val aSegmentProperty by segment(mockCodec, mockCodecContextTemplate)
                    }
                """.trimIndent())
            }
        }.isInstanceOf(ScriptException::class.java)
                .hasMessageContaining("property delegate must have a 'provideDelegate(Line_2.ExampleNonSegment, KProperty<*>)' method")
    }
}