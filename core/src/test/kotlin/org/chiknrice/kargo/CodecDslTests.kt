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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.ByteBuffer

@ExtendWith(MockKExtension::class)
class CodecDslTests {

    @Test
    fun `A codec factory produces a codec which delegates to the supplied encode and decode blocks`(
            @MockK mockEncode: EncodeBlock<Any>,
            @MockK mockDecode: DecodeBlock<Any>
    ) {
        val testValue = Any()
        val testBuffer = ByteBuffer.allocate(0)

        every { mockEncode(testValue, testBuffer) } just Runs
        every { mockDecode(testBuffer) } returns testValue

        val buildCodec = defineCodec<Any>() withEncoder mockEncode withDecoder mockDecode
        val codec = buildCodec()

        codec.encode(testValue, testBuffer)

        val result = codec.decode(testBuffer)

        assertThat(result).isSameAs(testValue)

        verify(exactly = 1) { mockEncode(testValue, testBuffer) }
        verify(exactly = 1) { mockDecode(testBuffer) }
        verifySequence {
            mockEncode(testValue, testBuffer)
            mockDecode(testBuffer)
        }

        confirmVerified(mockEncode, mockDecode)
    }

    @Test
    fun `A configurable codec factory produces a codec which delegates with config to the supplied encode and decode blocks`(
            @MockK mockEncodeWithConfig: EncodeWithConfigBlock<Any, Any>,
            @MockK mockDecodeWithConfig: DecodeWithConfigBlock<Any, Any>,
            @MockK mockSupplyDefaultConfig: SupplyDefaultConfigBlock<Any>,
            @MockK mockOverrideConfig: OverrideConfigBlock<Any>
    ) {
        val testConfig = Any()
        val testValue = Any()
        val testBuffer = ByteBuffer.allocate(0)

        every { mockSupplyDefaultConfig() } returns testConfig
        every { testConfig.mockOverrideConfig() } just Runs
        every { mockEncodeWithConfig(testValue, testBuffer, testConfig) } just Runs
        every { mockDecodeWithConfig(testBuffer, testConfig) } returns testValue

        val buildCodec = defineCodec<Any>() withConfig mockSupplyDefaultConfig withEncoder mockEncodeWithConfig withDecoder mockDecodeWithConfig
        val codec = buildCodec(mockOverrideConfig)

        codec.encode(testValue, testBuffer)

        val result = codec.decode(testBuffer)

        assertThat(result).isSameAs(testValue)

        verify(exactly = 1) { mockSupplyDefaultConfig() }
        verify(exactly = 1) { testConfig.mockOverrideConfig() }
        verify(exactly = 1) { mockEncodeWithConfig(testValue, testBuffer, testConfig) }
        verify(exactly = 1) { mockDecodeWithConfig(testBuffer, testConfig) }
        verifySequence {
            mockSupplyDefaultConfig()
            testConfig.mockOverrideConfig()
            mockEncodeWithConfig(testValue, testBuffer, testConfig)
            mockDecodeWithConfig(testBuffer, testConfig)
        }

        confirmVerified(mockSupplyDefaultConfig, mockOverrideConfig, mockEncodeWithConfig, mockDecodeWithConfig)
    }

    @Test
    fun `Each call to build the codec would result in a call to supplyDefaultConfig block and overrideConfig block`(
            @MockK mockEncodeWithConfig: EncodeWithConfigBlock<Any, Any>,
            @MockK mockDecodeWithConfig: DecodeWithConfigBlock<Any, Any>,
            @MockK mockSupplyDefaultConfig: SupplyDefaultConfigBlock<Any>,
            @MockK mockOverrideConfig: OverrideConfigBlock<Any>
    ) {
        val testConfig = Any()
        val testValue = Any()

        // just proving that 2 Any() objects are not the same
        assertThat(testValue).isNotSameAs(testConfig)

        every { mockSupplyDefaultConfig() } returns testConfig
        every { testConfig.mockOverrideConfig() } just Runs

        val buildCodec = defineCodec<Any>() withConfig mockSupplyDefaultConfig withEncoder mockEncodeWithConfig withDecoder mockDecodeWithConfig
        buildCodec(mockOverrideConfig)

        verify(exactly = 1) { mockSupplyDefaultConfig() }
        verify(exactly = 1) { testConfig.mockOverrideConfig() }

        buildCodec(mockOverrideConfig)

        verify(exactly = 2) { mockSupplyDefaultConfig() }
        verify(exactly = 2) { testConfig.mockOverrideConfig() }

        verifySequence {
            mockSupplyDefaultConfig()
            testConfig.mockOverrideConfig()
            mockSupplyDefaultConfig()
            testConfig.mockOverrideConfig()
        }

        confirmVerified(mockSupplyDefaultConfig, mockOverrideConfig, mockEncodeWithConfig, mockDecodeWithConfig)
    }


    @Test
    fun `If supplyDefaultConfig returns different objects for each call, each codec created would be associated with different config`(
            @MockK mockEncodeWithConfig: EncodeWithConfigBlock<Int, Int>,
            @MockK mockDecodeWithConfig: DecodeWithConfigBlock<Int, Int>
    ) {
        val testBuffer = ByteBuffer.allocate(0)

        every { mockEncodeWithConfig(any(), testBuffer, any()) } just Runs
        every { mockDecodeWithConfig(testBuffer, any()) } returns -1

        var configSource = 1

        val buildCodec = defineCodec<Int>() withConfig { configSource++ } withEncoder mockEncodeWithConfig withDecoder mockDecodeWithConfig

        val codec1 = buildCodec {}
        val codec2 = buildCodec {}

        codec1.encode(1, testBuffer)
        codec1.decode(testBuffer)

        verify { mockEncodeWithConfig(1, testBuffer, 1) }
        verify { mockDecodeWithConfig(testBuffer, 1) }

        codec2.encode(2, testBuffer)
        codec2.decode(testBuffer)

        verify { mockEncodeWithConfig(2, testBuffer, 2) }
        verify { mockDecodeWithConfig(testBuffer, 2) }

        confirmVerified(mockEncodeWithConfig, mockDecodeWithConfig)
    }

    @Test
    fun `If supplyDefaultConfig returns the same object for each call, different codecs created would be sharing the same config`(
            @MockK mockEncodeWithConfig: EncodeWithConfigBlock<Int, Int>,
            @MockK mockDecodeWithConfig: DecodeWithConfigBlock<Int, Int>
    ) {
        val testBuffer = ByteBuffer.allocate(0)

        every { mockEncodeWithConfig(any(), testBuffer, any()) } just Runs
        every { mockDecodeWithConfig(testBuffer, any()) } returns -1

        var configSource = 1

        val buildCodec = defineCodec<Int>() withConfig { configSource } withEncoder mockEncodeWithConfig withDecoder mockDecodeWithConfig

        val codec1 = buildCodec {}
        val codec2 = buildCodec {}

        codec1.encode(1, testBuffer)
        codec1.decode(testBuffer)

        verify { mockEncodeWithConfig(1, testBuffer, 1) }
        verify { mockDecodeWithConfig(testBuffer, 1) }

        codec2.encode(2, testBuffer)
        codec2.decode(testBuffer)

        verify { mockEncodeWithConfig(2, testBuffer, 1) }
        verify { mockDecodeWithConfig(testBuffer, 1) }

        confirmVerified(mockEncodeWithConfig, mockDecodeWithConfig)
    }

}