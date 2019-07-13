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
    fun `A codec delegates to the defined encode and decode blocks`(
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
    fun `A configurable codec delegates to the defined encode and decode blocks with the supplied config`(
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
            @MockK mockEncodeWithConfig: EncodeWithConfigBlock<Any, Any>,
            @MockK mockDecodeWithConfig: DecodeWithConfigBlock<Any, Any>
    ) {
        val configArg = slot<Any>()
        val testValue = Any()
        val testBuffer = ByteBuffer.allocate(0)

        every { mockEncodeWithConfig(testValue, testBuffer, capture(configArg)) } just Runs
        every { mockDecodeWithConfig(testBuffer, capture(configArg)) } returns testValue

        val buildCodec = defineCodec<Any>() withConfig { Any() } withEncoder mockEncodeWithConfig withDecoder mockDecodeWithConfig

        val codec1 = buildCodec {}
        val codec2 = buildCodec {}

        codec1.encode(testValue, testBuffer)
        val codec1EncodeConfig = configArg.captured

        codec1.decode(testBuffer)
        val codec1DecodeConfig = configArg.captured

        codec2.encode(testValue, testBuffer)
        val codec2EncodeConfig = configArg.captured

        codec2.decode(testBuffer)
        val codec2DecodeConfig = configArg.captured

        assertThat(codec1EncodeConfig).isSameAs(codec1DecodeConfig)
        assertThat(codec1EncodeConfig).isNotSameAs(codec2EncodeConfig)
        assertThat(codec1DecodeConfig).isNotSameAs(codec2DecodeConfig)
        assertThat(codec2EncodeConfig).isSameAs(codec2DecodeConfig)
    }

    @Test
    fun `If supplyDefaultConfig returns the same object for each call, different codecs created would be sharing the same config`(
            @MockK mockEncodeWithConfig: EncodeWithConfigBlock<Any, Any>,
            @MockK mockDecodeWithConfig: DecodeWithConfigBlock<Any, Any>
    ) {
        val configArg = slot<Any>()
        val testConfig = Any()
        val testValue = Any()
        val testBuffer = ByteBuffer.allocate(0)

        every { mockEncodeWithConfig(testValue, testBuffer, capture(configArg)) } just Runs
        every { mockDecodeWithConfig(testBuffer, capture(configArg)) } returns testValue

        val buildCodec = defineCodec<Any>() withConfig { testConfig } withEncoder mockEncodeWithConfig withDecoder mockDecodeWithConfig

        val codec1 = buildCodec {}
        val codec2 = buildCodec {}

        codec1.encode(testValue, testBuffer)
        val codec1EncodeConfig = configArg.captured

        codec1.decode(testBuffer)
        val codec1DecodeConfig = configArg.captured

        codec2.encode(testValue, testBuffer)
        val codec2EncodeConfig = configArg.captured

        codec2.decode(testBuffer)
        val codec2DecodeConfig = configArg.captured

        assertThat(codec1EncodeConfig).isSameAs(testConfig)
        assertThat(codec1DecodeConfig).isSameAs(testConfig)
        assertThat(codec2EncodeConfig).isSameAs(testConfig)
        assertThat(codec2DecodeConfig).isSameAs(testConfig)
    }

}

@ExtendWith(MockKExtension::class)
class FilterDslTests {

    @Test
    fun `A codec filter delegates to the defined encode and decode blocks with the wrapped codec`(
            @MockK(relaxed = true) mockCodec: Codec<Any>,
            @MockK mockFilterEncodeBlock: FilterEncodeBlock<Any>,
            @MockK mockFilterDecodeBlock: FilterDecodeBlock<Any>
    ) {
        val testValue = Any()
        val testBuffer = ByteBuffer.allocate(0)

        every { mockFilterEncodeBlock(testValue, testBuffer, mockCodec) } just Runs
        every { mockFilterDecodeBlock(testBuffer, mockCodec) } returns testValue

        val filterCodec = defineFilter<Any>() withEncoder mockFilterEncodeBlock withDecoder mockFilterDecodeBlock

        val filteredCodec = filterCodec(mockCodec)

        filteredCodec.encode(testValue, testBuffer)

        verify { mockFilterEncodeBlock(testValue, testBuffer, mockCodec) }

        val result = filteredCodec.decode(testBuffer)

        assertThat(result).isSameAs(testValue)

        verify { mockFilterDecodeBlock(testBuffer, mockCodec) }

        confirmVerified(mockFilterEncodeBlock, mockFilterDecodeBlock)
    }

}