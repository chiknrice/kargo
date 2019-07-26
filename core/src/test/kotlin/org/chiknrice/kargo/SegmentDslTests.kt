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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.ByteBuffer

@ExtendWith(MockKExtension::class)
class SegmentPropertyDefinitionTests {

    @MockK(relaxed = true)
    private lateinit var mockCodec: Codec<Any>
    @MockK
    private lateinit var mockCodecDefinition: CodecDefinition<Any>

    @BeforeAll
    fun setupMocks() {
        every { mockCodecDefinition.buildCodec() } returns mockCodec
    }

    @BeforeEach
    fun resetRecordedMockInteractions() {
        clearMocks(mockCodecDefinition, answers = false, childMocks = false, exclusionRules = false)
    }

    @Test
    fun `Segment properties should be delegated or else it won't be considered a segment property`() {
        class X : Segment() {
            var a = defineProperty<Any>() using mockCodecDefinition
        }

        val x = X()
        assertThat(x.properties).isEmpty()

        class Y : Segment() {
            var a by defineProperty<Any>() using mockCodecDefinition
        }

        val y = Y()
        assertThat(y.properties).isNotEmpty
    }

    @Test
    fun `Segments properties can be val or var and the value defaults to null`() {
        class X : Segment() {
            val a by defineProperty<Any>() using mockCodecDefinition
            var b by defineProperty<Any>() using mockCodecDefinition
        }

        val x = X()

        assertThat(x.a).isNull()
        assertThat(x.b).isNull()
    }

    @Test
    fun `The codec is only built when the segment is created`() {
        class X : Segment() {
            val a by defineProperty<Any>() using mockCodecDefinition
        }

        verify(exactly = 0) { mockCodecDefinition.buildCodec() }
        X()
        verify(exactly = 1) { mockCodecDefinition.buildCodec() }

        confirmVerified(mockCodecDefinition)
    }

    @Test
    fun `The codec is only built the first time the segment is created`() {
        class X : Segment() {
            val a by defineProperty<Any>() using mockCodecDefinition
        }

        verify(exactly = 0) { mockCodecDefinition.buildCodec() }
        X()
        verify(exactly = 1) { mockCodecDefinition.buildCodec() }
        X()
        verify(exactly = 1) { mockCodecDefinition.buildCodec() }

        confirmVerified(mockCodecDefinition)
    }

    @Test
    fun `The same codec builder used for different properties in the same segment creates different codecs for each property`() {
        class X : Segment() {
            val a by defineProperty<Any>() using mockCodecDefinition
            var b by defineProperty<Any>() using mockCodecDefinition
        }

        verify(exactly = 0) { mockCodecDefinition.buildCodec() }
        X()
        verify(exactly = 2) { mockCodecDefinition.buildCodec() }

        confirmVerified(mockCodecDefinition)
    }

    @Test
    fun `Segment classes only creates segment properties via segment dsl`() {
        class X : Segment() {
            val a by defineProperty<Any>() using mockCodecDefinition
            var b: Any = Any()
        }

        val x = X()

        assertThat(x.properties.size).isEqualTo(1)

        verify(exactly = 1) { mockCodecDefinition.buildCodec() }

        confirmVerified(mockCodecDefinition)
    }

    @Test
    fun `The order of segment properties maintain the order which they were defined in the class`() {
        class X : Segment() {
            val a by defineProperty<Any>() using mockCodecDefinition
            val b by defineProperty<Any>() using mockCodecDefinition
            val c by defineProperty<Any>() using mockCodecDefinition
        }

        val x = X()

        assertThat(x.properties.size).isEqualTo(3)
        assertThat(x.properties[0].context.kProperty.name).isEqualTo("a")
        assertThat(x.properties[1].context.kProperty.name).isEqualTo("b")
        assertThat(x.properties[2].context.kProperty.name).isEqualTo("c")

        verify(exactly = 3) { mockCodecDefinition.buildCodec() }

        confirmVerified(mockCodecDefinition)
    }

}

@ExtendWith(MockKExtension::class)
class SegmentPropertyCodecTests {

    @MockK
    private lateinit var mockCodec: Codec<Any>
    @MockK
    private lateinit var mockCodecDefinition: CodecDefinition<Any>
    @MockK(relaxed = true)
    private lateinit var mockBuffer: ByteBuffer
    private val testValue = Any()

    @BeforeAll
    fun setupMocks() {
        every { mockCodecDefinition.buildCodec() } returns mockCodec
        every { mockCodec.encode(testValue, mockBuffer) } just Runs
        every { mockCodec.decode(mockBuffer) } returns testValue
    }

    @BeforeEach
    fun resetRecordedMockInteractions() {
        clearMocks(mockCodecDefinition, mockCodec, answers = false, childMocks = false, exclusionRules = false)
    }

    @Test
    fun `Property encode method delegate to codec passing the current value`() {
        class X : Segment() {
            var a by defineProperty<Any>() using mockCodecDefinition
        }

        val x = X()
        x.a = testValue
        x.properties[0].encode(mockBuffer)

        verify(exactly = 1) { mockCodec.encode(testValue, mockBuffer) }

        confirmVerified(mockCodec)
    }

    @Test
    fun `Property encode method throws CodecException if property value is null`() {
        class X : Segment() {
            var a by defineProperty<Any>() using mockCodecDefinition
        }

        val x = X()
        assertThatThrownBy { x.properties[0].encode(mockBuffer) }.isExactlyInstanceOf(CodecException::class.java)
                .hasMessage("Encoding null property [X.a]")
    }

    @Test
    fun `Property decode method delegates to codec and sets the decoded value as the property's current value`() {
        class X : Segment() {
            var a by defineProperty<Any>() using mockCodecDefinition
        }

        val x = X()
        assertThat(x.a).isNull()
        x.properties[0].decode(mockBuffer)
        assertThat(x.a).isSameAs(testValue)

        verify(exactly = 1) { mockCodec.decode(mockBuffer) }

        confirmVerified(mockCodec)
    }

    @Test
    fun `A val property can still have a value after decoding`() {
        class X : Segment() {
            val a by defineProperty<Any>() using mockCodecDefinition
        }

        val x = X()
        assertThat(x.a).isNull()
        x.properties[0].decode(mockBuffer)
        assertThat(x.a).isSameAs(testValue)

        verify(exactly = 1) { mockCodec.decode(mockBuffer) }

        confirmVerified(mockCodec)
    }

    @Test
    // TODO: decommission this once segment codec has been implemented and tested because decoding always happens
    //   against a newly created segment class and all segment property values are null initially
    fun `Decoding a property overrides its current value`() {
        class X : Segment() {
            var a by defineProperty<Any>() using mockCodecDefinition
        }

        val x = X()
        x.a = Any()
        assertThat(x.a).isNotNull
        assertThat(x.a).isNotSameAs(testValue)
        x.properties[0].decode(mockBuffer)
        assertThat(x.a).isSameAs(testValue)

        verify(exactly = 1) { mockCodec.decode(mockBuffer) }

        confirmVerified(mockCodec)
    }

    @Test
    fun `Property codec wraps any exception thrown by the underlying codec with CodecException`(
            @MockK mockExceptionThrowingCodecDefinition: CodecDefinition<Any>,
            @MockK exceptionThrowingCodec: Codec<Any>
    ) {
        class SomeRandomException : Exception()
        every { mockExceptionThrowingCodecDefinition.buildCodec() } returns exceptionThrowingCodec
        every { exceptionThrowingCodec.encode(testValue, mockBuffer) } throws SomeRandomException()
        every { exceptionThrowingCodec.decode(mockBuffer) } throws SomeRandomException()

        class X : Segment() {
            var a by defineProperty<Any>() using mockExceptionThrowingCodecDefinition
        }

        val x = X()
        x.a = testValue
        assertThatThrownBy { x.properties[0].encode(mockBuffer) }
                .isExactlyInstanceOf(CodecException::class.java)
                .hasMessage("Error encoding [X.a]")
                .hasCauseExactlyInstanceOf(SomeRandomException::class.java)

        assertThatThrownBy { x.properties[0].decode(mockBuffer) }
                .isExactlyInstanceOf(CodecException::class.java)
                .hasMessage("Error decoding [X.a]")
                .hasCauseExactlyInstanceOf(SomeRandomException::class.java)
    }

    @Test
    fun `Configuration overrides allow configuration specified when codec was defined to be modified`(
            @MockK mockEncodeSpec: ConfigurableEncodeSpec<Any, Any>,
            @MockK mockDecodeSpec: ConfigurableDecodeSpec<Any, Any>
    ) {
        class Config {
            var length: Int = 4
        }

        val configArg = slot<Config>()

        every { mockEncodeSpec(testValue, mockBuffer, capture(configArg)) } just Runs
        every { mockDecodeSpec(mockBuffer, capture(configArg)) } returns testValue

        val codecDefinition = defineCodec<Any>() withConfig Config::class thatEncodesBy mockEncodeSpec andDecodesBy mockDecodeSpec

        class X : Segment() {
            var a by defineProperty<Any>() using codecDefinition
        }

        val x = X()
        x.a = testValue
        x.properties[0].encode(mockBuffer)

        assertThat(configArg.captured.length).isEqualTo(4)

        class Y : Segment() {
            var a by defineProperty<Any>() using codecDefinition withConfig { length = 10 }
        }

        val y = Y()
        y.a = testValue
        y.properties[0].encode(mockBuffer)

        assertThat(configArg.captured.length).isEqualTo(10)
    }

}

@ExtendWith(MockKExtension::class)
class SegmentPropertyCodecFilterTests {

    @MockK
    private lateinit var mockCodec: Codec<Any>
    @MockK
    private lateinit var mockFilteredCodec: Codec<Any>
    @MockK
    private lateinit var codecDefinition: CodecDefinition<Any>
    @MockK
    private lateinit var filterDefinition: FilterDefinition<Any>
    private val codecArg = slot<Codec<Any>>()
    @MockK(relaxed = true)
    private lateinit var mockBuffer: ByteBuffer
    private val testValue = Any()

    @BeforeAll
    fun setupMocks() {
        every { codecDefinition.buildCodec() } returns mockCodec
        every { filterDefinition.wrapCodec(capture(codecArg)) } returns mockFilteredCodec
        every { mockCodec.encode(testValue, mockBuffer) } just Runs
        every { mockCodec.decode(mockBuffer) } returns testValue
        every { mockFilteredCodec.encode(testValue, mockBuffer) } just Runs
        every { mockFilteredCodec.decode(mockBuffer) } returns testValue
    }

    @BeforeEach
    fun resetRecordedMockInteractions() {
        clearMocks(codecDefinition, filterDefinition, mockCodec, mockFilteredCodec, answers = false, childMocks = false,
                exclusionRules = false)
    }

    @Test
    fun `Property encode method delegates to the filter that wraps the codec passing the current value`() {
        class X : Segment() {
            var a by defineProperty<Any>() using codecDefinition
            var b by defineProperty<Any>() using codecDefinition wrappedWith filterDefinition
        }

        val x = X()

        verify { filterDefinition.wrapCodec(mockCodec) }

        x.a = testValue
        x.b = testValue

        x.properties[0].encode(mockBuffer)

        verify(exactly = 1) { mockCodec.encode(testValue, mockBuffer) }
        verify(exactly = 0) { mockFilteredCodec.encode(testValue, mockBuffer) }

        x.properties[1].encode(mockBuffer)

        verify(exactly = 1) { mockCodec.encode(testValue, mockBuffer) }
        verify(exactly = 1) { mockFilteredCodec.encode(testValue, mockBuffer) }

        confirmVerified(filterDefinition, mockFilteredCodec)
    }

    @Test
    fun `The last filter wraps the previous one`(
            @MockK lastMockFilterDefinition: FilterDefinition<Any>,
            @MockK lastMockFilteredCodec: Codec<Any>
    ) {
        every { lastMockFilterDefinition.wrapCodec(mockFilteredCodec) } returns lastMockFilteredCodec
        every { lastMockFilteredCodec.encode(testValue, mockBuffer) } just Runs

        class X : Segment() {
            var a by defineProperty<Any>() using codecDefinition wrappedWith filterDefinition thenWith lastMockFilterDefinition
        }

        val x = X()
        x.a = testValue

        verify { lastMockFilterDefinition.wrapCodec(mockFilteredCodec) }

        x.properties[0].encode(mockBuffer)

        verify(exactly = 0) { mockCodec.encode(testValue, mockBuffer) }
        verify(exactly = 0) { mockFilteredCodec.encode(testValue, mockBuffer) }
        verify(exactly = 1) { lastMockFilteredCodec.encode(testValue, mockBuffer) }

        confirmVerified(mockCodec, mockFilteredCodec, lastMockFilteredCodec)
    }

    @Test
    fun `Configuration overrides allow configuration specified when codec filter was defined to be modified`(
            @MockK mockFilterEncodeSpec: ConfigurableFilterEncodeSpec<Any, Any>,
            @MockK mockFilterDecodeSpec: ConfigurableFilterDecodeSpec<Any, Any>
    ) {
        class Config {
            var length: Int = 4
        }

        val configArg = slot<Config>()

        every { mockFilterEncodeSpec(testValue, mockBuffer, capture(configArg), mockCodec) } just Runs
        every { mockFilterEncodeSpec(testValue, mockBuffer, capture(configArg), mockFilteredCodec) } just Runs

        val lastFilterDefinition = defineFilter<Any>() withConfig Config::class thatEncodesBy mockFilterEncodeSpec andDecodesBy mockFilterDecodeSpec

        class W : Segment() {
            var a by defineProperty<Any>() using codecDefinition wrappedWith lastFilterDefinition
        }

        val w = W()
        w.a = testValue
        w.properties[0].encode(mockBuffer)

        assertThat(configArg.captured.length).isEqualTo(4)

        class X : Segment() {
            var a by defineProperty<Any>() using codecDefinition wrappedWith filterDefinition thenWith lastFilterDefinition
        }

        val x = X()
        x.a = testValue
        x.properties[0].encode(mockBuffer)

        assertThat(configArg.captured.length).isEqualTo(4)

        class Y : Segment() {
            var a by defineProperty<Any>() using codecDefinition wrappedWith lastFilterDefinition withConfig { length = 10 }
        }

        val y = Y()
        y.a = testValue
        y.properties[0].encode(mockBuffer)

        assertThat(configArg.captured.length).isEqualTo(10)

        class Z : Segment() {
            var a by defineProperty<Any>() using codecDefinition wrappedWith filterDefinition thenWith lastFilterDefinition withConfig { length = 10 }
        }

        val z = Z()
        z.a = testValue
        z.properties[0].encode(mockBuffer)

        assertThat(configArg.captured.length).isEqualTo(10)
    }

}