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

import kotlin.reflect.KClass

object SegmentCodecs : Definition() {

    fun <T : Segment> simple(segmentClass: KClass<T>) = segmentCodec(segmentClass) {
        encode { _, segmentProperties, buffer ->
            segmentProperties.forEach { it.encode(buffer) }
        }
        decode { segmentProperties, buffer, _ ->
            segmentProperties.forEach { it.decode(buffer) }
        }
    }

    inline fun <reified T : Segment> simple() = simple(T::class)

}
