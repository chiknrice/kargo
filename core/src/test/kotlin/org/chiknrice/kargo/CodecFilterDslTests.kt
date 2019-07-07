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
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class CodecFilterDslTests {

    private val dummyBuffer = ByteBuffer.allocate(0)
    private val dummyContext = codecContextTemplate { }.createNew()
    private val mockCodec = mockk<Codec<String>>()

    @Test
    fun `A codec filter definition without any onDecode and onEncode blocks will result in ConfigurationException`() {
        assertThatThrownBy {
            filter<String> { }
        }.isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Incomplete codec filter definition")

        assertThatThrownBy {
            filter<String> {
                onDecodeFilter { _, _, _ -> "some dummy result" }
            }
        }.isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Incomplete codec filter definition")

        assertThatThrownBy {
            filter<String> {
                onEncodeFilter { _, _, _, _ -> }
            }
        }.isInstanceOf(ConfigurationException::class.java)
                .hasMessage("Incomplete codec filter definition")
    }

    @Test
    fun `A complete codec filter definition should return a filter block and not a codec`() {
        val aFilter = filter<String> {
            onDecodeFilter { _, _, _ -> "some dummy result" }
            onEncodeFilter { _, _, _, _ -> }
        }

        assertThat(aFilter).isNotInstanceOfAny(Codec::class.java)
    }

    @Test
    fun `Filtering a codec would result in a different codec instance`() {
        val aFilter = filter<String> {
            onDecodeFilter { _, _, _ -> "some dummy result" }
            onEncodeFilter { _, _, _, _ -> }
        }

        val filteredCodec = mockCodec.filterWith(aFilter)
        assertThat(filteredCodec).isInstanceOf(Codec::class.java)
        assertThat(filteredCodec).isNotSameAs(mockCodec)
    }

    @Test
    fun `A the filtered codec should receive the encode and decode parameters together with the wrapped codec as the chain`() {
        val aFilter = filter<String> {
            onDecodeFilter { buffer, context, chain ->
                assertThat(buffer).isSameAs(dummyBuffer)
                assertThat(context).isSameAs(dummyContext)
                assertThat(chain).isSameAs(mockCodec)
                "some dummy result"
            }
            onEncodeFilter { value, buffer, context, chain ->
                assertThat(value).isEqualTo("Some string to encode")
                assertThat(buffer).isSameAs(dummyBuffer)
                assertThat(context).isSameAs(dummyContext)
                assertThat(chain).isSameAs(mockCodec)
            }
        }

        val filteredCodec = mockCodec.filterWith(aFilter)
        filteredCodec.encode("Some string to encode", dummyBuffer, dummyContext)
        val decodedValue = filteredCodec.decode(dummyBuffer, dummyContext)
        assertThat(decodedValue).isEqualTo("some dummy result")

        confirmVerified(mockCodec)
    }

    @Test
    fun `Chained filters should be called in the order they were wrapped`() {
        val callOrder = mutableListOf<String>()

        val mockResult = "Decoded result"
        every { mockCodec.decode(dummyBuffer, dummyContext) } answers {
            callOrder.add("codec decode")
            mockResult
        }
        every { mockCodec.encode(any(), dummyBuffer, dummyContext) } answers {
            callOrder.add("codec encode")
        }

        val firstFilter = filter<String> {
            onDecodeFilter { buffer, context, chain ->
                callOrder.add("firstFilter.decode in")
                chain.decode(buffer, context).also { callOrder.add("firstFilter.decode out") }
            }
            onEncodeFilter { value, buffer, context, chain ->
                callOrder.add("firstFilter.encode in")
                chain.encode(value, buffer, context)
                callOrder.add("firstFilter.encode out")
            }
        }

        val lastFilter = filter<String> {
            onDecodeFilter { buffer, context, chain ->
                callOrder.add("lastFilter.decode in")
                chain.decode(buffer, context).also { callOrder.add("lastFilter.decode out") }
            }
            onEncodeFilter { value, buffer, context, chain ->
                callOrder.add("lastFilter.encode in")
                chain.encode(value, buffer, context)
                callOrder.add("lastFilter.encode out")
            }
        }

        val filteredCodec = mockCodec.filterWith(firstFilter).filterWith(lastFilter)

        val valueToEncode = "Value to encode"

        filteredCodec.encode(valueToEncode, dummyBuffer, dummyContext)

        verify { mockCodec.encode(valueToEncode, dummyBuffer, dummyContext) }

        assertThat(callOrder).isEqualTo(listOf(
                "lastFilter.encode in",
                "firstFilter.encode in",
                "codec encode",
                "firstFilter.encode out",
                "lastFilter.encode out"))

        callOrder.clear()

        val decodedValue = filteredCodec.decode(dummyBuffer, dummyContext)

        assertThat(decodedValue).isSameAs(mockResult)

        verify { mockCodec.decode(dummyBuffer, dummyContext) }

        assertThat(callOrder).isEqualTo(listOf(
                "lastFilter.decode in",
                "firstFilter.decode in",
                "codec decode",
                "firstFilter.decode out",
                "lastFilter.decode out"))

        confirmVerified(mockCodec)
    }

}