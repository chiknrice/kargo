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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer


class CodecDslTests {

    val mockEncode = mockk<EncodeBlock<Any>>()
    val mockDecode = mockk<DecodeBlock<Any>>()
    val mockEncodeWithConfig = mockk<EncodeWithConfigBlock<Any, Any>>()
    val mockDecodeWithConfig = mockk<DecodeWithConfigBlock<Any, Any>>()
    val mockSupplyDefaultConfig = mockk<SupplyDefaultConfigBlock<Any>>()
    val mockOverrideConfig = mockk<OverrideConfigBlock<Any>>()

    @BeforeEach
    fun resetMocks() {
        clearAllMocks()
    }

    @Test
    fun `A codec factory produces a codec which delegates to the supplied encode and decode blocks`() {
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
    fun `A configurable codec factory produces a codec which delegates with config to the supplied encode and decode blocks`() {
        val testConfig = Any()
        val testValue = Any()
        val testBuffer = ByteBuffer.allocate(0)

        // just proving that 2 Any() objects are not the same
        assertThat(testValue).isNotSameAs(testConfig)

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

}