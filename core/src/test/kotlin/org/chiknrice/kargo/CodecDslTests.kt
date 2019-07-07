/*
 * Copyright (c) 2019 Ian Bondoc
 *
 * This file is part of project "kargo"
 *
 * Project kargo is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or(at your option) any later version.
 *
 * Project kargo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 */

package org.chiknrice.kargo

import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class CodecDslTests {

    @Test
    fun `A codec definition without any onDecode and onEncode blocks will result in ConfigurationException`() {
        assertThatThrownBy {
            codec<String> {
            }
        }.isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Incomplete codec definition")

        assertThatThrownBy {
            codec<String> {
                onEncode { _, _, _ -> }
            }
        }.isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Incomplete codec definition")

        assertThatThrownBy {
            codec<String> {
                onDecode { _, _ -> "some dummy result" }
            }
        }.isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Incomplete codec definition")

    }

    @Test
    fun `A complete codec definition should return a valid codec delegating to the supplied codec blocks`() {
        val dummyBuffer = ByteBuffer.allocate(0)
        val dummyContext = codecContextTemplate {  }.createNew()

        val delegate = mockk<Codec<String>>()

        val mockResult = "Decoded result"
        every { delegate.decode(dummyBuffer, dummyContext) } returns mockResult

        every { delegate.encode(any(), dummyBuffer, dummyContext) } just Runs

        val codec = codec<String> {
            onEncode { value, buffer, context -> delegate.encode(value, buffer, context) }
            onDecode { buffer, context -> delegate.decode(buffer, context) }
        }

        val valueToEncode = "Value to encode"
        codec.encode(valueToEncode, dummyBuffer, dummyContext)

        verify { delegate.encode(valueToEncode, dummyBuffer, dummyContext) }

        val decoded = codec.decode(dummyBuffer, dummyContext)
        assertThat(decoded).isSameAs(mockResult)

        verify { delegate.decode(dummyBuffer, dummyContext) }

        confirmVerified(delegate)
    }

}