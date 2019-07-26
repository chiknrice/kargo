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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.ByteBuffer

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

        val codecDefinition = defineCodec<Any>() thatEncodesBy mockEncodeSpec andDecodesBy mockDecodeSpec
        val codec = codecDefinition.buildCodec()

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
    fun `A configuration class without a default constructor results in exception`(
            @MockK mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>
    ) {
        class X(var a: Any)

        val codecDefinition = defineCodec<Any>() withConfig defineConfig<X>() thatEncodesBy mockEncodeSpec andDecodesBy mockDecodeSpec
        assertThatThrownBy { codecDefinition.buildCodec() }.isExactlyInstanceOf(CodecConfigurationException::class.java)
                .hasMessage("Failed to create configuration class instance: X")
                .hasCauseExactlyInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `A configurable codec delegates to the defined encode and decode specs with the same instance of the config class`(
            @MockK mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>
    ) {
        every { mockEncodeSpec(testValue, mockBuffer, capture(configArg)) } just Runs
        every { mockDecodeSpec(mockBuffer, capture(configArg)) } returns testValue

        val codecDefinition = defineCodec<Any>() withConfig defineConfig(
                Any::class) thatEncodesBy mockEncodeSpec andDecodesBy mockDecodeSpec
        val codec = codecDefinition.buildCodec()

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
    fun `A codec config override spec applies to the config`(
            @MockK(relaxed = true) mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK(relaxed = true) mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>,
            @MockK mockConfigSpec: ConfigSpec<Any>
    ) {
        every { capture(configArg).mockConfigSpec() } just Runs

        val codecDefinition = defineCodec<Any>() withConfig defineConfig(
                Any::class) thatEncodesBy mockEncodeSpec andDecodesBy mockDecodeSpec
        codecDefinition.withOverrides(mockConfigSpec).buildCodec()

        val config = configArg.captured

        verify(exactly = 1) { config.mockConfigSpec() }

        confirmVerified(mockConfigSpec)
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

        val filterDefinition = defineFilter<Any>() thatEncodesBy mockFilterEncodeSpec andDecodesBy mockFilterDecodeSpec

        val filteredCodec = filterDefinition.wrapCodec(mockCodec)

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

        val filterDefinition = defineFilter<Any>() withConfig defineConfig(
                Any::class) thatEncodesBy mockFilterEncodeSpec andDecodesBy mockFilterDecodeSpec
        val codec = filterDefinition.wrapCodec(mockCodec)

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

        val filterDefinition = defineFilter<Any>() withConfig defineConfig(
                Any::class) thatEncodesBy mockFilterEncodeSpec andDecodesBy mockFilterDecodeSpec
        filterDefinition.withOverrides(mockConfigSpec).wrapCodec(mockCodec)

        val config = configArg.captured

        verify(exactly = 1) { config.mockConfigSpec() }

        confirmVerified(mockConfigSpec)
    }

}

@ExtendWith(MockKExtension::class)
class SegmentCodecDslTests {

    // somehow mocks that are member of the test class is causing an issue with createInstance reflection method called
    // on segment classes having segment properties defined with these mocks
    companion object {
        val staticMockCodecDefinition = mockk<CodecDefinition<Any>>()
        private val staticMockCodec = mockk<Codec<Any>>(relaxed = true)

        init {
            every { staticMockCodecDefinition.buildCodec() } returns staticMockCodec
        }
    }

    @MockK(relaxed = true)
    private lateinit var mockBuffer: ByteBuffer
    @MockK(relaxed = true)
    private lateinit var mockEncodeSegmentSpec: EncodeSegmentSpec<Any>
    @MockK(relaxed = true)
    private lateinit var mockDecodeSegmentSpec: DecodeSegmentSpec<Any>

    @Test
    fun `Defining a segment codec for a segment class without a default constructor results in exception`() {
        class X(val a: Any = "") : Segment() {
            var b by defineProperty<Any>() using staticMockCodecDefinition
        }

        class Y(val a: Any) : Segment() {
            var b by defineProperty<Any>() using staticMockCodecDefinition
        }

        // segment class with constructor argument but with default values should still work
        defineSegmentCodec<X>() thatEncodesBy mockEncodeSegmentSpec andDecodesBy mockDecodeSegmentSpec

        assertThatThrownBy {
            defineSegmentCodec<Y>() thatEncodesBy mockEncodeSegmentSpec andDecodesBy mockDecodeSegmentSpec
        }.isExactlyInstanceOf(CodecConfigurationException::class.java)
                .hasMessage("Failed to create segment class instance: Y")
                .hasCauseExactlyInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `Defining a segment codec for a segment class having segment properties assigned and not delegated results in exception`() {
        class X : Segment() {
            var a by defineProperty<Any>() using staticMockCodecDefinition
            var b = Any()
            var c = defineProperty<Any>() using staticMockCodecDefinition
            var d = defineProperty<Any>() using staticMockCodecDefinition
        }
        assertThatThrownBy {
            defineSegmentCodec<X>() thatEncodesBy mockEncodeSegmentSpec andDecodesBy mockDecodeSegmentSpec
        }.isExactlyInstanceOf(CodecConfigurationException::class.java)
                .hasMessage("Properties are incorrectly assigned: [c, d]")
    }

    @Test
    fun `Defining a segment codec for a segment class without any segment properties results in exception`() {
        class X : Segment() {
            var a = Any()
        }
        assertThatThrownBy {
            defineSegmentCodec<X>() thatEncodesBy mockEncodeSegmentSpec andDecodesBy mockDecodeSegmentSpec
        }.isExactlyInstanceOf(CodecConfigurationException::class.java)
                .hasMessage("Segment class [X] is required to have a segment property")
    }

    @Test
    fun `Segment codec delegates to the defined encode and decode specs passing an instance of the segment class`() {
        class X : Segment() {
            var a by defineProperty<Any>() using staticMockCodecDefinition
        }

        val codecDefinition = defineSegmentCodec<X>() thatEncodesBy mockEncodeSegmentSpec andDecodesBy mockDecodeSegmentSpec

        val segmentCodec = codecDefinition.buildCodec()
        val testSegment = X()
        segmentCodec.encode(testSegment, mockBuffer)

        val decodedSegment = segmentCodec.decode(mockBuffer)

        verify { mockEncodeSegmentSpec(testSegment, any(), mockBuffer) }
        verify { mockDecodeSegmentSpec(any(), mockBuffer, decodedSegment) }

        confirmVerified(mockEncodeSegmentSpec, mockDecodeSegmentSpec)
    }

    @Test
    @Disabled("actually not needed as segment instances are always created")
    fun `Root segment codec resets all values prior to decoding`() {
        TODO("implement this")
    }

}