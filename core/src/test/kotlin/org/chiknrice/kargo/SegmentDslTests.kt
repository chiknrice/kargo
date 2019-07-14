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

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SegmentPropertyDslTests {

    @MockK(relaxed = true)
    private lateinit var mockCodec: Codec<Any>

    @Test
    fun `Segments properties can be val or var and the value defaults to null`(
            @MockK mockBuildCodec: BuildCodecBlock<Any>
    ) {
        every { mockBuildCodec() } returns mockCodec

        class X : Segment() {
            val y by defineProperty<Any>() using mockBuildCodec
            var z by defineProperty<Any>() using mockBuildCodec
        }

        val x = X()

        assertThat(x.y).isNull()
        assertThat(x.z).isNull()
    }

    @Test
    fun `The codec is only built when the segment is created`(
            @MockK mockBuildCodec: BuildCodecBlock<Any>
    ) {
        every { mockBuildCodec() } returns mockCodec

        class X : Segment() {
            val y by defineProperty<Any>() using mockBuildCodec
        }

        verify(exactly = 0) { mockBuildCodec() }
        X()
        verify(exactly = 1) { mockBuildCodec() }

        confirmVerified(mockBuildCodec)
    }

    @Test
    fun `The codec is only built the first time the segment is created`(
            @MockK mockBuildCodec: BuildCodecBlock<Any>
    ) {
        every { mockBuildCodec() } returns mockCodec

        class X : Segment() {
            val y by defineProperty<Any>() using mockBuildCodec
        }

        verify(exactly = 0) { mockBuildCodec() }
        X()
        verify(exactly = 1) { mockBuildCodec() }
        X()
        verify(exactly = 1) { mockBuildCodec() }

        confirmVerified(mockBuildCodec)
    }

    @Test
    fun `The same codec builder used for different properties in the same segment creates different codecs for each`(
            @MockK mockBuildCodec: BuildCodecBlock<Any>
    ) {
        every { mockBuildCodec() } returns mockCodec

        class X : Segment() {
            val y by defineProperty<Any>() using mockBuildCodec
            var z by defineProperty<Any>() using mockBuildCodec
        }

        verify(exactly = 0) { mockBuildCodec() }
        X()
        verify(exactly = 2) { mockBuildCodec() }

        confirmVerified(mockBuildCodec)
    }

    @Test
    @Disabled
    fun `Segment classes only creates segment properties via segment dsl`() {
        TODO("implement this")
    }

    @Test
    @Disabled
    fun `The order of segment properties maintain the order which they were defined in the class`() {
        TODO("implement this")
    }

    @Test
    @Disabled
    fun `Configuration overrides allow configuration specified when codec was defined to be modified`() {
        TODO("implement this")
    }

    @Test
    @Disabled
    fun `Property encode and decode methods delegate to the codec`() {
        TODO("implement this")
    }

    @Test
    @Disabled
    fun `Property encode method delegate to codec passing the current value`() {
        TODO("implement this")
    }

    @Test
    @Disabled
    fun `Property decode method delegates to codec and sets the decoded value as the property's current value`() {
        TODO("implement this")
    }

    @Test
    @Disabled
    fun `Subsequent filters wraps the previous filter`() {
        TODO("implement this")
    }
}