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

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.ByteBuffer

@ExtendWith(MockKExtension::class)
class SimpleSegmentCodecTests {

    // somehow having the mocks as a member, method param, or local class causes createInstance of segment class to fail
    companion object {
        var mockCodecADef = mockk<CodecDefinition<Any>>()
        var mockCodecBDef = mockk<CodecDefinition<Any>>()
        var mockCodecCDef = mockk<CodecDefinition<Any>>()
        var mockCodecA = mockk<Codec<Any>>(relaxed = true)
        var mockCodecB = mockk<Codec<Any>>(relaxed = true)
        var mockCodecC = mockk<Codec<Any>>(relaxed = true)

        init {
            every { mockCodecADef.buildCodec() } returns mockCodecA
            every { mockCodecBDef.buildCodec() } returns mockCodecB
            every { mockCodecCDef.buildCodec() } returns mockCodecC
        }
    }

    @Test
    fun `Simple segment codec encodes all properties in order they were defined`(
            @MockK(relaxed = true) mockBuffer: ByteBuffer
    ) {
        class X : Segment() {
            var a by codec(mockCodecADef)
            var b by codec(mockCodecBDef)
            var c by codec(mockCodecCDef)
        }

        val a = Any()
        val b = Any()
        val c = Any()

        val x = X()
        x.a = a
        x.b = b
        x.c = c

        val segmentCodec = SegmentCodecs.simple<X>().buildCodec()
        segmentCodec.encode(x, mockBuffer)
        segmentCodec.decode(mockBuffer)

        verifySequence {
            mockCodecA.encode(a, mockBuffer)
            mockCodecB.encode(b, mockBuffer)
            mockCodecC.encode(c, mockBuffer)
            mockCodecA.decode(mockBuffer)
            mockCodecB.decode(mockBuffer)
            mockCodecC.decode(mockBuffer)
        }
    }

}