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

import org.junit.jupiter.api.Test


// config cannot be a data class with constructor params (but with defaults) if we want to use constructor reference!
// or else type inference fails
class StringConfig {
    var length: Int = -1
}

val stringCodec = codecFor<String>() withConfig ::StringConfig withEncoder { value, buffer, config -> } withDecoder { buffer, config ->
    TODO("implement this")
}

class RootSegment : Segment() {
    var element1 by segment<String> {
        withCodec(stringCodec) {
            length = 4
        }
    }
}

class CodecDslTestsx {

    @Test
    fun `Try creating one`() {
        RootSegment()
    }

}