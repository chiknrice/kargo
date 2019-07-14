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
    private val configArg = slot<Any>()

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
    fun `A codec defined either encoder or decoder first behaves the same`(
            @MockK mockEncode: EncodeBlock<Any>,
            @MockK mockDecode: DecodeBlock<Any>
    ) {
        every { mockEncode(testValue, mockBuffer) } just Runs
        every { mockDecode(mockBuffer) } returns testValue

        val codec1 = (defineCodec<Any>() thatEncodesBy mockEncode andDecodesBy mockDecode)()
        val codec2 = (defineCodec<Any>() thatDecodesBy mockDecode andEncodesBy mockEncode)()

        codec1.encode(testValue, mockBuffer)
        codec2.encode(testValue, mockBuffer)

        verify(exactly = 2) { mockEncode(testValue, mockBuffer) }

        val decode1 = codec1.decode(mockBuffer)
        val decode2 = codec2.decode(mockBuffer)

        assertThat(decode1).isSameAs(decode2)

        verify(exactly = 2) { mockDecode(mockBuffer) }

        confirmVerified(mockEncode, mockDecode)
    }

    @Test
    fun `A configurable codec delegates to the defined encode and decode blocks with the same instance of the config class`(
            @MockK mockEncode: EncodeWithConfigBlock<Any, Any>,
            @MockK mockDecode: DecodeWithConfigBlock<Any, Any>
    ) {
        every { mockEncode(testValue, mockBuffer, capture(configArg)) } just Runs
        every { mockDecode(mockBuffer, capture(configArg)) } returns testValue

        val buildCodec = defineCodec<Any>() withConfig Any::class thatEncodesBy mockEncode andDecodesBy mockDecode
        val codec = buildCodec {}

        codec.encode(testValue, mockBuffer)
        val encodeConfig = configArg.captured

        val result = codec.decode(mockBuffer)
        val decodeConfig = configArg.captured

        assertThat(result).isSameAs(testValue)

        assertThat(encodeConfig).isSameAs(decodeConfig)

        verify { mockEncode(testValue, mockBuffer, encodeConfig) }
        verify { mockDecode(mockBuffer, decodeConfig) }

        confirmVerified(mockEncode, mockDecode)
    }

    @Test
    fun `A configurable codec defined either encoder or decoder first behaves the same but with each their different instance of config`(
            @MockK mockEncode: EncodeWithConfigBlock<Any, Any>,
            @MockK mockDecode: DecodeWithConfigBlock<Any, Any>
    ) {
        every { mockEncode(testValue, mockBuffer, capture(configArg)) } just Runs
        every { mockDecode(mockBuffer, capture(configArg)) } returns testValue

        val codec1 = (defineCodec<Any>() withConfig Any::class thatEncodesBy mockEncode andDecodesBy mockDecode) {}
        val codec2 = (defineCodec<Any>() withConfig Any::class thatDecodesBy mockDecode andEncodesBy mockEncode) {}

        codec1.encode(testValue, mockBuffer)
        val encode1Config = configArg.captured
        codec2.encode(testValue, mockBuffer)
        val encode2Config = configArg.captured

        verify(exactly = 1) { mockEncode(testValue, mockBuffer, encode1Config) }
        verify(exactly = 1) { mockEncode(testValue, mockBuffer, encode2Config) }

        assertThat(encode1Config).isNotSameAs(encode2Config)

        val decode1 = codec1.decode(mockBuffer)
        val decode1Config = configArg.captured
        val decode2 = codec2.decode(mockBuffer)
        val decode2Config = configArg.captured

        assertThat(decode1).isSameAs(decode2)

        verify(exactly = 1) { mockDecode(mockBuffer, decode1Config) }
        verify(exactly = 1) { mockDecode(mockBuffer, decode2Config) }

        assertThat(decode1Config).isNotSameAs(decode2Config)

        assertThat(encode1Config).isSameAs(decode1Config)
        assertThat(encode2Config).isSameAs(decode2Config)

        confirmVerified(mockEncode, mockDecode)
    }

//    @Test
//    fun `Each call to build the codec would result in a call to supplyDefaultConfig block and overrideConfig block`(
//            @MockK mockEncode: EncodeWithConfigBlock<Any, Any>,
//            @MockK mockDecode: DecodeWithConfigBlock<Any, Any>,
//            @MockK mockOverrideConfig: OverrideConfigBlock<Any>
//    ) {
//        every { Any::class.createInstance() } returns testConfig
//        every { testConfig.mockOverrideConfig() } just Runs
//
//        val buildCodec = defineCodec<Any>() withConfig Any::class thatEncodesBy mockEncode andDecodesBy mockDecode
//        buildCodec(mockOverrideConfig)
//
//        verify(exactly = 1) { Any::class.createInstance() }
//        verify(exactly = 1) { testConfig.mockOverrideConfig() }
//
//        buildCodec(mockOverrideConfig)
//
//        verify(exactly = 2) { Any::class.createInstance() }
//        verify(exactly = 2) { testConfig.mockOverrideConfig() }
//
//        verifySequence {
//            Any::class.createInstance()
//            testConfig.mockOverrideConfig()
//            Any::class.createInstance()
//            testConfig.mockOverrideConfig()
//        }
//
//        confirmVerified(Any::class, mockOverrideConfig, mockEncode, mockDecode)
//    }

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