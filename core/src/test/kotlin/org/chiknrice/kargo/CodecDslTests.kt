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

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.chiknrice.kargo.StaticMocks.staticMockCodecDefinition
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.ByteBuffer

// somehow mocks that are member of the test class is causing an issue with createInstance reflection method called
// on segment classes having segment properties defined with these mocks
object StaticMocks {
    val staticMockCodecDefinition = mockk<CodecDefinition<Any>>()
    private val staticMockCodec = mockk<Codec<Any>>(relaxed = true)

    init {
        every { staticMockCodecDefinition.buildCodec() } returns staticMockCodec
    }
}

@ExtendWith(MockKExtension::class)
class IncompleteDefinitionTests {

    @Test
    fun `A codec definition requires at least an encode and a decode spec`(
            @MockK(relaxed = true) mockEncodeSpec: EncodeSpec<Any>,
            @MockK(relaxed = true) mockDecodeSpec: DecodeSpec<Any>
    ) {
        with(object : Definition() {}) {
            assertThatThrownBy {
                codec<Any> {
                    encode(mockEncodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Decode spec is required")

            assertThatThrownBy {
                codec<Any> {
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Encode spec is required")

            assertThatThrownBy {
                codec<Any> {
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("""
                        Errors:
                         - Encode spec is required
                         - Decode spec is required
                    """.trimIndent())
        }
    }

    @Test
    fun `A configurable codec definition requires at least an encode and a decode spec`(
            @MockK(relaxed = true) mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>
    ) {
        with(object : Definition() {}) {
            assertThatThrownBy {
                codec<Any, Any> {
                    encode(mockEncodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Decode spec is required")

            assertThatThrownBy {
                codec<Any, Any> {
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Encode spec is required")

            assertThatThrownBy {
                codec<Any, Any> {
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("""
                        Errors:
                         - Encode spec is required
                         - Decode spec is required
                    """.trimIndent())
        }
    }

    @Test
    fun `A filter definition requires at least an encode and a decode spec`(
            @MockK(relaxed = true) mockEncodeSpec: FilterEncodeSpec<Any>,
            @MockK(relaxed = true) mockDecodeSpec: FilterDecodeSpec<Any>
    ) {
        with(object : Definition() {}) {
            assertThatThrownBy {
                filter<Any> {
                    encode(mockEncodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Decode spec is required")

            assertThatThrownBy {
                filter<Any> {
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Encode spec is required")

            assertThatThrownBy {
                filter<Any> {
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("""
                        Errors:
                         - Encode spec is required
                         - Decode spec is required
                    """.trimIndent())
        }
    }

    @Test
    fun `A configurable filter definition requires at least an encode and a decode spec`(
            @MockK(relaxed = true) mockEncodeSpec: ConfigurableFilterEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableFilterDecodeSpec<Any, Any>
    ) {
        with(object : Definition() {}) {
            assertThatThrownBy {
                filter<Any, Any> {
                    encode(mockEncodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Decode spec is required")

            assertThatThrownBy {
                filter<Any, Any> {
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Encode spec is required")

            assertThatThrownBy {
                filter<Any, Any> {
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("""
                        Errors:
                         - Encode spec is required
                         - Decode spec is required
                    """.trimIndent())

        }
    }

    @Test
    fun `A segment codec definition requires at least an encode and a decode spec`(
            @MockK(relaxed = true) mockEncodeSpec: EncodeSegmentSpec<Any>,
            @MockK(relaxed = true) mockDecodeSpec: DecodeSegmentSpec<Any>
    ) {
        class X : Segment() {
            var a by defineProperty<Any>() using staticMockCodecDefinition
        }

        with(object : Definition() {}) {
            assertThatThrownBy {
                segmentCodec<X> {
                    encode(mockEncodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Decode spec is required")

            assertThatThrownBy {
                segmentCodec<X> {
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Encode spec is required")

            assertThatThrownBy {
                segmentCodec<X> {
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("""
                        Errors:
                         - Encode spec is required
                         - Decode spec is required
                    """.trimIndent())
        }
    }

}

@ExtendWith(MockKExtension::class)
class MultiDeclarationTests {

    @Test
    fun `Declaring a codec's encode or decode spec multiple times results in exception`(
            @MockK(relaxed = true) mockEncodeSpec: EncodeSpec<Any>,
            @MockK(relaxed = true) mockDecodeSpec: DecodeSpec<Any>
    ) {
        with(object : Definition() {}) {
            assertThatThrownBy {
                codec<Any> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    encode(mockEncodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Encode spec declared multiple times")

            assertThatThrownBy {
                codec<Any> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Decode spec declared multiple times")

            assertThatThrownBy {
                codec<Any> {
                    encode(mockEncodeSpec)
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("""
                        Errors:
                         - Encode spec declared multiple times
                         - Decode spec declared multiple times
                    """.trimIndent())
        }
    }

    @Test
    fun `Declaring a configurable codec's encode or decode spec multiple times results in exception`(
            @MockK(relaxed = true) mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>
    ) {
        with(object : Definition() {}) {
            assertThatThrownBy {
                codec<Any, Any> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    encode(mockEncodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Encode spec declared multiple times")

            assertThatThrownBy {
                codec<Any, Any> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Decode spec declared multiple times")

            assertThatThrownBy {
                codec<Any, Any> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    config {}
                    config {}
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Config spec declared multiple times")

            assertThatThrownBy {
                codec<Any, Any> {
                    encode(mockEncodeSpec)
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    decode(mockDecodeSpec)
                    config {}
                    config {}
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("""
                        Errors:
                         - Encode spec declared multiple times
                         - Decode spec declared multiple times
                         - Config spec declared multiple times
                    """.trimIndent())
        }
    }

    @Test
    fun `Declaring a filter's encode or decode spec multiple times results in exception`(
            @MockK(relaxed = true) mockEncodeSpec: FilterEncodeSpec<Any>,
            @MockK(relaxed = true) mockDecodeSpec: FilterDecodeSpec<Any>
    ) {
        with(object : Definition() {}) {
            assertThatThrownBy {
                filter<Any> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    encode(mockEncodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Encode spec declared multiple times")

            assertThatThrownBy {
                filter<Any> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Decode spec declared multiple times")

            assertThatThrownBy {
                filter<Any> {
                    encode(mockEncodeSpec)
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("""
                        Errors:
                         - Encode spec declared multiple times
                         - Decode spec declared multiple times
                    """.trimIndent())
        }
    }

    @Test
    fun `Declaring a configurable filter's encode or decode spec multiple times results in exception`(
            @MockK(relaxed = true) mockEncodeSpec: ConfigurableFilterEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableFilterDecodeSpec<Any, Any>
    ) {
        with(object : Definition() {}) {
            assertThatThrownBy {
                filter<Any, Any> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    encode(mockEncodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Encode spec declared multiple times")

            assertThatThrownBy {
                filter<Any, Any> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Decode spec declared multiple times")

            assertThatThrownBy {
                filter<Any, Any> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    config {}
                    config {}
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Config spec declared multiple times")

            assertThatThrownBy {
                filter<Any, Any> {
                    encode(mockEncodeSpec)
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    decode(mockDecodeSpec)
                    config {}
                    config {}
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("""
                        Errors:
                         - Encode spec declared multiple times
                         - Decode spec declared multiple times
                         - Config spec declared multiple times
                    """.trimIndent())
        }
    }

    @Test
    fun `Declaring a segment codec's encode or decode spec multiple times results in exception`(
            @MockK(relaxed = true) mockEncodeSpec: EncodeSegmentSpec<Any>,
            @MockK(relaxed = true) mockDecodeSpec: DecodeSegmentSpec<Any>
    ) {
        class X : Segment() {
            var a by defineProperty<Any>() using staticMockCodecDefinition
        }

        with(object : Definition() {}) {
            assertThatThrownBy {
                segmentCodec<X> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    encode(mockEncodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Encode spec declared multiple times")

            assertThatThrownBy {
                segmentCodec<X> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Decode spec declared multiple times")

            assertThatThrownBy {
                segmentCodec<X> {
                    encode(mockEncodeSpec)
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("""
                        Errors:
                         - Encode spec declared multiple times
                         - Decode spec declared multiple times
                    """.trimIndent())
        }
    }

}

@ExtendWith(MockKExtension::class)
class RequiredNoArgConstructorTests {

    @Test
    fun `A codec's configuration class without a no-arg constructor results in exception`(
            @MockK(relaxed = true) mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>
    ) {
        class X(var a: Any = Any())
        class Y(var a: Any)

        with(object : Definition() {}) {
            // config with default values are allowed
            codec<Any, X> {
                encode(mockEncodeSpec)
                decode(mockDecodeSpec)
            }

            assertThatThrownBy {
                codec<Any, Y> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Configuration class Y does not have a no-arg constructor")
        }
    }

    @Test
    fun `A filter's configuration class without a no-arg constructor results in exception`(
            @MockK(relaxed = true) mockEncodeSpec: ConfigurableFilterEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableFilterDecodeSpec<Any, Any>,
            @MockK(relaxed = true) mockCodec: Codec<Any>
    ) {
        class X(var a: Any = Any())
        class Y(var a: Any)

        with(object : Definition() {}) {
            filter<Any, X> {
                encode(mockEncodeSpec)
                decode(mockDecodeSpec)
            }.wrapCodec(mockCodec)

            assertThatThrownBy {
                filter<Any, Y> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                }.wrapCodec(mockCodec)
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Configuration class Y does not have a no-arg constructor")
        }
    }

    @Test
    fun `Defining a segment codec for a segment class without a no-arg constructor results in exception`(
            @MockK(relaxed = true) mockEncodeSpec: EncodeSegmentSpec<Any>,
            @MockK(relaxed = true) mockDecodeSpec: DecodeSegmentSpec<Any>
    ) {
        class X(val a: Any = Any()) : Segment() {
            var b by defineProperty<Any>() using staticMockCodecDefinition
        }

        class Y(val a: Any) : Segment() {
            var b by defineProperty<Any>() using staticMockCodecDefinition
        }

        with(object : Definition() {}) {
            // segment class with constructor argument but with default values should still work
            segmentCodec<X> {
                encode(mockEncodeSpec)
                decode(mockDecodeSpec)
            }

            assertThatThrownBy {
                segmentCodec<Y> {
                    encode(mockEncodeSpec)
                    decode(mockDecodeSpec)
                }
            }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                    .hasMessage("Segment class Y does not have a no-arg constructor")
        }
    }

}

@ExtendWith(MockKExtension::class)
class CodecDslTests {

    @MockK(relaxed = true)
    private lateinit var mockBuffer: ByteBuffer
    private val testValue = Any()
    private val configArg = slot<Any>()

    @Test
    fun `A codec delegates to the defined encode and decode specs`(
            @MockK mockEncodeSpec: EncodeSpec<Any>,
            @MockK mockDecodeSpec: DecodeSpec<Any>
    ) {
        every { mockEncodeSpec(testValue, mockBuffer) } just Runs
        every { mockDecodeSpec(mockBuffer) } returns testValue

        val codecs = object : Definition() {
            val codecDefinition = codec<Any> {
                encode(mockEncodeSpec)
                decode(mockDecodeSpec)
            }
        }

        val codec = codecs.codecDefinition.buildCodec()

        codec.encode(testValue, mockBuffer)

        val result = codec.decode(mockBuffer)

        assertThat(result).isSameAs(testValue)

        verify(exactly = 1) { mockEncodeSpec(testValue, mockBuffer) }
        verify(exactly = 1) { mockDecodeSpec(mockBuffer) }
        verifySequence {
            mockEncodeSpec(testValue, mockBuffer)
            mockDecodeSpec(mockBuffer)
        }

        confirmVerified(mockEncodeSpec, mockDecodeSpec)
    }

    @Test
    fun `A configurable codec delegates to the defined encode and decode specs with the same instance of the config class`(
            @MockK mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>
    ) {
        every { mockEncodeSpec(testValue, mockBuffer, capture(configArg)) } just Runs
        every { mockDecodeSpec(mockBuffer, capture(configArg)) } returns testValue

        val codecs = object : Definition() {
            val codecDefinition = codec<Any, Any> {
                encode(mockEncodeSpec)
                decode(mockDecodeSpec)
            }
        }
        val codec = codecs.codecDefinition.buildCodec()

        codec.encode(testValue, mockBuffer)
        val encodeConfig = configArg.captured

        val result = codec.decode(mockBuffer)
        val decodeConfig = configArg.captured

        assertThat(result).isSameAs(testValue)

        assertThat(encodeConfig).isSameAs(decodeConfig)

        verify { mockEncodeSpec(testValue, mockBuffer, encodeConfig) }
        verify { mockDecodeSpec(mockBuffer, decodeConfig) }

        confirmVerified(mockEncodeSpec, mockDecodeSpec)
    }

    @Test
    fun `An initial codec config spec applies to the config`(
            @MockK(relaxed = true) mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>,
            @MockK mockConfigSpec: ConfigSpec<Any>
    ) {
        every { capture(configArg).mockConfigSpec() } just Runs

        val codecs = object : Definition() {
            val codecDefinition = codec<Any, Any> {
                encode(mockEncodeSpec)
                decode(mockDecodeSpec)
                config(mockConfigSpec)
            }
        }

        codecs.codecDefinition.buildCodec()

        val config = configArg.captured

        verify(exactly = 1) { config.mockConfigSpec() }

        confirmVerified(mockConfigSpec)
    }

    @Test
    fun `A codec config override spec applies to the config`(
            @MockK(relaxed = true) mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>,
            @MockK mockConfigSpec: ConfigSpec<Any>
    ) {
        every { capture(configArg).mockConfigSpec() } just Runs

        val codecs = object : Definition() {
            val codecDefinition = codec<Any, Any> {
                encode(mockEncodeSpec)
                decode(mockDecodeSpec)
            }
        }

        codecs.codecDefinition.withOverrides(mockConfigSpec).buildCodec()

        val config = configArg.captured

        verify(exactly = 1) { config.mockConfigSpec() }

        confirmVerified(mockConfigSpec)
    }

    @Test
    fun `A codec config override applies to the config after the initial config specification is applied`(
            @MockK(relaxed = true) mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>,
            @MockK mockConfigSpec: ConfigSpec<Any>,
            @MockK mockConfigOverrideSpec: ConfigSpec<Any>
    ) {
        every { capture(configArg).mockConfigSpec() } just Runs
        every { capture(configArg).mockConfigOverrideSpec() } just Runs

        val codecs = object : Definition() {
            val codecDefinition = codec<Any, Any> {
                encode(mockEncodeSpec)
                decode(mockDecodeSpec)
                config(mockConfigSpec)
            }
        }

        codecs.codecDefinition.withOverrides(mockConfigOverrideSpec).buildCodec()

        val config = configArg.captured

        verifySequence {
            config.mockConfigSpec()
            config.mockConfigOverrideSpec()
        }

        confirmVerified(mockConfigSpec, mockConfigOverrideSpec)
    }

    @Test
    fun `A configurable codec without a config spec will have a configuration of the class defaults`(
            @MockK mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>
    ) {
        data class X(var a: Int = 1, var b: String = "orig")

        val configArg = slot<X>()
        every { mockEncodeSpec(testValue, mockBuffer, capture(configArg)) } just Runs

        val codecs = object : Definition() {
            val codecDefinition = codec<Any, X> {
                encode(mockEncodeSpec)
                decode(mockDecodeSpec)
            }
        }

        val codec = codecs.codecDefinition.buildCodec()

        codec.encode(testValue, mockBuffer)
        val config = configArg.captured
        assertThat(config).isEqualTo(X())
    }

    @Test
    fun `A configurable codec with a config spec will have a modified configuration reflecting the spec changes`(
            @MockK mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>
    ) {
        data class X(var a: Int = 1, var b: String = "orig")

        val configArg = slot<X>()
        every { mockEncodeSpec(testValue, mockBuffer, capture(configArg)) } just Runs

        val codecs = object : Definition() {
            val codecDefinition = codec<Any, X> {
                encode(mockEncodeSpec)
                decode(mockDecodeSpec)
                config {
                    b = "overridden"
                }
            }
        }

        val codec = codecs.codecDefinition.buildCodec()

        codec.encode(testValue, mockBuffer)
        val config = configArg.captured
        assertThat(config).isEqualTo(X(b = "overridden"))
    }

    @Test
    fun `A codec created from the original definition is not affected by a prior config override`(
            @MockK mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>
    ) {
        class X(var a: Int = 1)

        val configArg = slot<X>()
        every { mockEncodeSpec(testValue, mockBuffer, capture(configArg)) } just Runs

        val codecs = object : Definition() {
            val codecDefinition = codec<Any, X> {
                encode(mockEncodeSpec)
                decode(mockDecodeSpec)
            }
            val overriddenCodecDefinition = codecDefinition.withOverrides { a = 2 }
        }
        val originalCodec = codecs.codecDefinition.buildCodec()
        val overriddenCodec = codecs.overriddenCodecDefinition.buildCodec()

        originalCodec.encode(testValue, mockBuffer)
        val originalEncodeConfig = configArg.captured
        assertThat(originalEncodeConfig.a).isEqualTo(1)

        overriddenCodec.encode(testValue, mockBuffer)
        val overriddenEncodeConfig = configArg.captured
        assertThat(overriddenEncodeConfig.a).isEqualTo(2)

        verifySequence {
            mockEncodeSpec(testValue, mockBuffer, originalEncodeConfig)
            mockEncodeSpec(testValue, mockBuffer, overriddenEncodeConfig)
        }

        confirmVerified(mockEncodeSpec)
    }

    @Test
    fun `A subsequent config override creates a new codec definition`(
            @MockK(relaxed = true) mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>
    ) {
        class X(var a: Int = 1)

        val codecs = object : Definition() {
            val codecDefinition = codec<Any, X> {
                encode(mockEncodeSpec)
                decode(mockDecodeSpec)
            }
            val overriddenCodecDefinition = codecDefinition.withOverrides {}
        }

        with(codecs) {
            assertThat(overriddenCodecDefinition).isNotSameAs(codecDefinition)
        }
    }

    @Test
    fun `A subsequent config override creates another definition which supersedes the previous settings`(
            @MockK mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>
    ) {
        class X(var a: Int = 1)

        val configArg = slot<X>()
        every { mockEncodeSpec(testValue, mockBuffer, capture(configArg)) } just Runs

        val codecs = object : Definition() {
            val codecDefinition = codec<Any, X> {
                encode(mockEncodeSpec)
                decode(mockDecodeSpec)
                config {
                    a = 2
                }
            }
            val overriddenDefinition = codecDefinition.withOverrides { a = 5 }
        }

        val overriddenCodec = codecs.overriddenDefinition.buildCodec()

        overriddenCodec.encode(testValue, mockBuffer)

        val overriddenEncodeConfig = configArg.captured

        assertThat(overriddenEncodeConfig.a).isEqualTo(5)
    }

}

@ExtendWith(MockKExtension::class)
class FilterDslTests {

    @MockK(relaxed = true)
    private lateinit var mockBuffer: ByteBuffer
    @MockK(relaxed = true)
    private lateinit var mockCodec: Codec<Any>
    private val testValue = Any()
    private val configArg = slot<Any>()

    @Test
    fun `A codec filter delegates to the defined encode and decode specs with the wrapped codec`(
            @MockK mockFilterEncodeSpec: FilterEncodeSpec<Any>,
            @MockK mockFilterDecodeSpec: FilterDecodeSpec<Any>
    ) {
        every { mockFilterEncodeSpec(testValue, mockBuffer, mockCodec) } just Runs
        every { mockFilterDecodeSpec(mockBuffer, mockCodec) } returns testValue

        val filters = object : Definition() {
            val filterDefinition = filter<Any> {
                encode(mockFilterEncodeSpec)
                decode(mockFilterDecodeSpec)
            }
        }

        val filteredCodec = filters.filterDefinition.wrapCodec(mockCodec)

        filteredCodec.encode(testValue, mockBuffer)

        verify { mockFilterEncodeSpec(testValue, mockBuffer, mockCodec) }

        val result = filteredCodec.decode(mockBuffer)

        assertThat(result).isSameAs(testValue)

        verify { mockFilterDecodeSpec(mockBuffer, mockCodec) }

        confirmVerified(mockFilterEncodeSpec, mockFilterDecodeSpec)
    }

    @Test
    fun `A configurable codec filter delegates to the defined encode and decode specs with the same instance of the config class`(
            @MockK mockFilterEncodeSpec: ConfigurableFilterEncodeSpec<Any, Any>,
            @MockK mockFilterDecodeSpec: ConfigurableFilterDecodeSpec<Any, Any>
    ) {
        every { mockFilterEncodeSpec(testValue, mockBuffer, capture(configArg), mockCodec) } just Runs
        every { mockFilterDecodeSpec(mockBuffer, capture(configArg), mockCodec) } returns testValue

        val filters = object : Definition() {
            val filterDefinition = filter<Any, Any> {
                encode(mockFilterEncodeSpec)
                decode(mockFilterDecodeSpec)
            }
        }
        val codec = filters.filterDefinition.wrapCodec(mockCodec)

        codec.encode(testValue, mockBuffer)
        val encodeConfig = configArg.captured

        val result = codec.decode(mockBuffer)
        val decodeConfig = configArg.captured

        assertThat(result).isSameAs(testValue)

        assertThat(encodeConfig).isSameAs(decodeConfig)

        verify { mockFilterEncodeSpec(testValue, mockBuffer, encodeConfig, mockCodec) }
        verify { mockFilterDecodeSpec(mockBuffer, decodeConfig, mockCodec) }

        confirmVerified(mockFilterEncodeSpec, mockFilterDecodeSpec)
    }

    @Test
    fun `A codec filter config override spec applies to the config`(
            @MockK(relaxed = true) mockFilterEncodeSpec: ConfigurableFilterEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockFilterDecodeSpec: ConfigurableFilterDecodeSpec<Any, Any>,
            @MockK mockConfigSpec: ConfigSpec<Any>
    ) {
        every { capture(configArg).mockConfigSpec() } just Runs

        val filters = object : Definition() {
            val filterDefinition = filter<Any, Any> {
                encode(mockFilterEncodeSpec)
                decode(mockFilterDecodeSpec)
            }
        }
        filters.filterDefinition.withOverrides(mockConfigSpec).wrapCodec(mockCodec)

        val config = configArg.captured

        verify(exactly = 1) { config.mockConfigSpec() }

        confirmVerified(mockConfigSpec)
    }

}

@ExtendWith(MockKExtension::class)
class SegmentCodecDslTests {

    @MockK(relaxed = true)
    private lateinit var mockBuffer: ByteBuffer
    @MockK(relaxed = true)
    private lateinit var mockEncodeSegmentSpec: EncodeSegmentSpec<Any>
    @MockK(relaxed = true)
    private lateinit var mockDecodeSegmentSpec: DecodeSegmentSpec<Any>

    @Test
    fun `Defining a segment codec for a segment class having segment properties assigned and not delegated results in exception`() {
        class X : Segment() {
            var a by defineProperty<Any>() using staticMockCodecDefinition
            var b = Any()
            var c = defineProperty<Any>() using staticMockCodecDefinition
            var d = defineProperty<Any>() using staticMockCodecDefinition
        }
        assertThatThrownBy {
            object : Definition() {
                val def = segmentCodec<X> {
                    encode(mockEncodeSegmentSpec)
                    decode(mockDecodeSegmentSpec)
                }
            }
        }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                .hasMessage("Properties are incorrectly assigned: [c, d]")
    }

    @Test
    fun `Defining a segment codec for a segment class without any segment properties results in exception`() {
        class X : Segment() {
            var a = Any()
        }
        assertThatThrownBy {
            object : Definition() {
                val def = segmentCodec<X> {
                    encode(mockEncodeSegmentSpec)
                    decode(mockDecodeSegmentSpec)
                }
            }
        }.isExactlyInstanceOf(CodecDefinitionException::class.java)
                .hasMessage("Segment class [X] is required to have a segment property")
    }

    @Test
    fun `Segment codec delegates to the defined encode and decode specs passing an instance of the segment class`() {
        class X : Segment() {
            var a by defineProperty<Any>() using staticMockCodecDefinition
        }

        val codecs = object : Definition() {
            val codecDefinition = segmentCodec<X> {
                encode(mockEncodeSegmentSpec)
                decode(mockDecodeSegmentSpec)
            }
        }

        val segmentCodec = codecs.codecDefinition.buildCodec()
        val testSegment = X()
        segmentCodec.encode(testSegment, mockBuffer)

        val decodedSegment = segmentCodec.decode(mockBuffer)

        verify { mockEncodeSegmentSpec(testSegment, any(), mockBuffer) }
        verify { mockDecodeSegmentSpec(any(), mockBuffer, decodedSegment) }

        confirmVerified(mockEncodeSegmentSpec, mockDecodeSegmentSpec)
    }

    @Test
    @Disabled("actually not needed as segment instances are always created")
    // Change to test showing that segment properties are replaced with new instance
    fun `Root segment codec resets all values prior to decoding`() {
        TODO("implement this")
    }

}