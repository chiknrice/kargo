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

    @MockK(relaxed = true)
    private lateinit var mockBuffer: ByteBuffer
    private val testValue = Any()
    private val testConfig = Any()

    @Test
    fun `A codec delegates to the defined encode and decode blocks`(
            @MockK mockEncode: EncodeBlock<Any>,
            @MockK mockDecode: DecodeBlock<Any>
    ) {

        every { mockEncode(testValue, mockBuffer) } just Runs
        every { mockDecode(mockBuffer) } returns testValue

        val buildCodec = defineCodec<Any>() thatEncodesBy mockEncode andDecodesBy mockDecode
        val codec = buildCodec()

        codec.encode(testValue, mockBuffer)

        val result = codec.decode(mockBuffer)

        assertThat(result).isSameAs(testValue)

        verify(exactly = 1) { mockEncode(testValue, mockBuffer) }
        verify(exactly = 1) { mockDecode(mockBuffer) }
        verifySequence {
            mockEncode(testValue, mockBuffer)
            mockDecode(mockBuffer)
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

        every { mockSupplyDefaultConfig() } returns testConfig
        every { testConfig.mockOverrideConfig() } just Runs
        every { mockEncodeWithConfig(testValue, mockBuffer, testConfig) } just Runs
        every { mockDecodeWithConfig(mockBuffer, testConfig) } returns testValue

        val buildCodec = defineCodec<Any>() withConfig mockSupplyDefaultConfig thatEncodesBy mockEncodeWithConfig andDecodesBy mockDecodeWithConfig
        val codec = buildCodec(mockOverrideConfig)

        codec.encode(testValue, mockBuffer)

        val result = codec.decode(mockBuffer)

        assertThat(result).isSameAs(testValue)

        verify(exactly = 1) { mockSupplyDefaultConfig() }
        verify(exactly = 1) { testConfig.mockOverrideConfig() }
        verify(exactly = 1) { mockEncodeWithConfig(testValue, mockBuffer, testConfig) }
        verify(exactly = 1) { mockDecodeWithConfig(mockBuffer, testConfig) }
        verifySequence {
            mockSupplyDefaultConfig()
            testConfig.mockOverrideConfig()
            mockEncodeWithConfig(testValue, mockBuffer, testConfig)
            mockDecodeWithConfig(mockBuffer, testConfig)
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
        every { mockSupplyDefaultConfig() } returns testConfig
        every { testConfig.mockOverrideConfig() } just Runs

        val buildCodec = defineCodec<Any>() withConfig mockSupplyDefaultConfig thatEncodesBy mockEncodeWithConfig andDecodesBy mockDecodeWithConfig
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

        every { mockEncodeWithConfig(testValue, mockBuffer, capture(configArg)) } just Runs
        every { mockDecodeWithConfig(mockBuffer, capture(configArg)) } returns testValue

        val buildCodec = defineCodec<Any>() withConfig { Any() } thatEncodesBy mockEncodeWithConfig andDecodesBy mockDecodeWithConfig

        val codec1 = buildCodec {}
        val codec2 = buildCodec {}

        codec1.encode(testValue, mockBuffer)
        val codec1EncodeConfig = configArg.captured

        codec1.decode(mockBuffer)
        val codec1DecodeConfig = configArg.captured

        codec2.encode(testValue, mockBuffer)
        val codec2EncodeConfig = configArg.captured

        codec2.decode(mockBuffer)
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

        every { mockEncodeWithConfig(testValue, mockBuffer, capture(configArg)) } just Runs
        every { mockDecodeWithConfig(mockBuffer, capture(configArg)) } returns testValue

        val buildCodec = defineCodec<Any>() withConfig { testConfig } thatEncodesBy mockEncodeWithConfig andDecodesBy mockDecodeWithConfig

        val codec1 = buildCodec {}
        val codec2 = buildCodec {}

        codec1.encode(testValue, mockBuffer)
        val codec1EncodeConfig = configArg.captured

        codec1.decode(mockBuffer)
        val codec1DecodeConfig = configArg.captured

        codec2.encode(testValue, mockBuffer)
        val codec2EncodeConfig = configArg.captured

        codec2.decode(mockBuffer)
        val codec2DecodeConfig = configArg.captured

        assertThat(codec1EncodeConfig).isSameAs(testConfig)
        assertThat(codec1DecodeConfig).isSameAs(testConfig)
        assertThat(codec2EncodeConfig).isSameAs(testConfig)
        assertThat(codec2DecodeConfig).isSameAs(testConfig)
    }

}

@ExtendWith(MockKExtension::class)
class FilterDslTests {

    @MockK(relaxed = true)
    private lateinit var mockBuffer: ByteBuffer
    @MockK(relaxed = true)
    private lateinit var mockCodec: Codec<Any>
    private val testValue = Any()

    @Test
    fun `A codec filter delegates to the defined encode and decode blocks with the wrapped codec`(
            @MockK mockFilterEncodeBlock: FilterEncodeBlock<Any>,
            @MockK mockFilterDecodeBlock: FilterDecodeBlock<Any>
    ) {
        every { mockFilterEncodeBlock(testValue, mockBuffer, mockCodec) } just Runs
        every { mockFilterDecodeBlock(mockBuffer, mockCodec) } returns testValue

        val filterCodec = defineFilter<Any>() thatEncodesBy mockFilterEncodeBlock andDecodesBy mockFilterDecodeBlock

        val filteredCodec = filterCodec(mockCodec)

        filteredCodec.encode(testValue, mockBuffer)

        verify { mockFilterEncodeBlock(testValue, mockBuffer, mockCodec) }

        val result = filteredCodec.decode(mockBuffer)

        assertThat(result).isSameAs(testValue)

        verify { mockFilterDecodeBlock(mockBuffer, mockCodec) }

        confirmVerified(mockFilterEncodeBlock, mockFilterDecodeBlock)
    }

}