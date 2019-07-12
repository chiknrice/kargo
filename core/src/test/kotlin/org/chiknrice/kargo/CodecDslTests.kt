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
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer


class CodecDslTests {

    val mockCodecDelegate = mockk<Codec<String>>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(mockCodecDelegate)
    }

    @Test
    fun `A codec factory produces a codec which delegates to the supplied encode and decode blocks`() {
        val testValue = "test"
        val testBuffer = ByteBuffer.allocate(0)

        every { mockCodecDelegate.encode(testValue, testBuffer) } just Runs
        every { mockCodecDelegate.decode(testBuffer) } returns testValue

        val buildCodec = defineCodec<String>() withEncoder { value, buffer ->
            mockCodecDelegate.encode(value, buffer)
        } withDecoder { buffer ->
            mockCodecDelegate.decode(buffer)
        }
        val codec = buildCodec()

        codec.encode(testValue, testBuffer)

        verify { mockCodecDelegate.encode(testValue, testBuffer) }

        val result = codec.decode(testBuffer)

        Assertions.assertThat(result).isSameAs(testValue)

        verify { mockCodecDelegate.decode(testBuffer) }

    }

}