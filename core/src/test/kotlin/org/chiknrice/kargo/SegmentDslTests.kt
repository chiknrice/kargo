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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.ByteBuffer

@ExtendWith(MockKExtension::class)
class SegmentPropertyDefinitionTests {

    @MockK(relaxed = true)
    private lateinit var mockCodec: Codec<Any>
    @MockK
    private lateinit var mockBuildCodec: BuildCodecBlock<Any>

    @BeforeAll
    fun setupMocks() {
        every { mockBuildCodec() } returns mockCodec
    }

    @BeforeEach
    fun resetRecordedInteractions() {
        clearMocks(mockBuildCodec, answers = false, childMocks = false, exclusionRules = false)
    }

    @Test
    fun `Segments properties can be val or var and the value defaults to null`() {
        class X : Segment() {
            val a by defineProperty<Any>() using mockBuildCodec
            var b by defineProperty<Any>() using mockBuildCodec
        }

        val x = X()

        assertThat(x.a).isNull()
        assertThat(x.b).isNull()
    }

    @Test
    fun `The codec is only built when the segment is created`() {
        class X : Segment() {
            val a by defineProperty<Any>() using mockBuildCodec
        }

        verify(exactly = 0) { mockBuildCodec() }
        X()
        verify(exactly = 1) { mockBuildCodec() }

        confirmVerified(mockBuildCodec)
    }

    @Test
    fun `The codec is only built the first time the segment is created`() {
        class X : Segment() {
            val a by defineProperty<Any>() using mockBuildCodec
        }

        verify(exactly = 0) { mockBuildCodec() }
        X()
        verify(exactly = 1) { mockBuildCodec() }
        X()
        verify(exactly = 1) { mockBuildCodec() }

        confirmVerified(mockBuildCodec)
    }

    @Test
    fun `The same codec builder used for different properties in the same segment creates different codecs for each property`() {
        class X : Segment() {
            val a by defineProperty<Any>() using mockBuildCodec
            var b by defineProperty<Any>() using mockBuildCodec
        }

        verify(exactly = 0) { mockBuildCodec() }
        X()
        verify(exactly = 2) { mockBuildCodec() }

        confirmVerified(mockBuildCodec)
    }

    @Test
    fun `Segment classes only creates segment properties via segment dsl`() {
        class X : Segment() {
            val a by defineProperty<Any>() using mockBuildCodec
            var b: Any = Any()
        }

        val x = X()

        assertThat(x.properties.size).isEqualTo(1)

        verify(exactly = 1) { mockBuildCodec() }

        confirmVerified(mockBuildCodec)
    }

    @Test
    fun `The order of segment properties maintain the order which they were defined in the class`() {
        class X : Segment() {
            val a by defineProperty<Any>() using mockBuildCodec
            val b by defineProperty<Any>() using mockBuildCodec
            val c by defineProperty<Any>() using mockBuildCodec
        }

        val x = X()

        assertThat(x.properties.size).isEqualTo(3)
        assertThat(x.properties[0].kProperty.name).isEqualTo("a")
        assertThat(x.properties[1].kProperty.name).isEqualTo("b")
        assertThat(x.properties[2].kProperty.name).isEqualTo("c")

        verify(exactly = 3) { mockBuildCodec() }

        confirmVerified(mockBuildCodec)
    }


    @Test
    @Disabled
    fun `Subsequent filters wraps the previous filter`() {
        TODO("implement this")
    }
}

@ExtendWith(MockKExtension::class)
class SegmentPropertyCodecTests {

    @MockK
    private lateinit var mockCodec: Codec<Any>
    @MockK
    private lateinit var mockBuildCodec: BuildCodecBlock<Any>
    @MockK(relaxed = true)
    private lateinit var mockBuffer: ByteBuffer
    private val testValue = Any()

    @BeforeAll
    fun setupMocks() {
        every { mockBuildCodec() } returns mockCodec
        every { mockCodec.encode(testValue, mockBuffer) } just Runs
        every { mockCodec.decode(mockBuffer) } returns testValue
    }

    @Test
    fun `Property encode method delegate to codec passing the current value`() {
        class X : Segment() {
            var a by defineProperty<Any>() using mockBuildCodec
        }

        val x = X()
        x.a = testValue
        x.properties[0].encode(mockBuffer)

        verify(exactly = 1) { mockCodec.encode(testValue, mockBuffer) }

        confirmVerified(mockCodec)
    }

    @Test
    fun `Property decode method delegates to codec and sets the decoded value as the property's current value`() {
        class X : Segment() {
            var a by defineProperty<Any>() using mockBuildCodec
        }

        val x = X()
        assertThat(x.a).isNull()
        x.properties[0].decode(mockBuffer)
        assertThat(x.a).isEqualTo(testValue)

        verify(exactly = 1) { mockCodec.decode(mockBuffer) }

        confirmVerified(mockCodec)
    }

    @Test
    fun `Configuration overrides allow configuration specified when codec was defined to be modified`(
            @MockK mockEncode: EncodeWithConfigBlock<Any, Any>,
            @MockK mockDecode: DecodeWithConfigBlock<Any, Any>
    ) {
        class Config {
            var length: Int = 4
        }

        val configArg = slot<Config>()

        every { mockEncode(testValue, mockBuffer, capture(configArg)) } just Runs
        every { mockDecode(mockBuffer, capture(configArg)) } returns testValue

        val configurableCodec = defineCodec<Any>() withConfig Config::class thatEncodesBy mockEncode andDecodesBy mockDecode

        class X : Segment() {
            var a by defineProperty<Any>() using configurableCodec
        }

        val x = X()
        x.a = testValue
        x.properties[0].encode(mockBuffer)

        assertThat(configArg.captured.length).isEqualTo(4)

        class Y : Segment() {
            var a by defineProperty<Any>() using configurableCodec withConfig { length = 10 }
        }

        val y = Y()
        y.a = testValue
        y.properties[0].encode(mockBuffer)

        assertThat(configArg.captured.length).isEqualTo(10)
    }

    @Test
    @Disabled
    fun `A val property can still have a value after decoding`() {

    }

    @Test
    @Disabled
    fun `Decoding a property overrides its current value`() {

    }

    @Test
    @Disabled
    fun `Encoding a null property results in CodecException`() {

    }


}