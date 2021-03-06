/**
 * Copyright (c) 2020, Saalfeld Lab, HHMI Janelia
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// auto-generated, do not modify!

@file:Suppress("UNCHECKED_CAST")

package net.imglib2.imklib

import kotlin.Number
import kotlin.Suppress
import net.imglib2.RandomAccessible
import net.imglib2.type.logic.BoolType
import net.imglib2.type.numeric.IntegerType
import net.imglib2.type.numeric.RealType

infix fun RandomAccessible<out RealType<*>>.ge(that: RealType<*>): RandomAccessible<BoolType> {
    return when {
        this.type is IntegerType && that is IntegerType -> (this as RandomAccessible<IntegerType<*>>).convert(BoolType()) { s, t -> t.set(s.integerLong >= that.integerLong) }
        else -> convert(BoolType()) { s, t -> t.set(s.realDouble >= that.realDouble) }
    }
}

infix fun RealType<*>.ge(that: RandomAccessible<out RealType<*>>): RandomAccessible<BoolType> {
    return that le this
}

infix fun RandomAccessible<out RealType<*>>.ge(that: Number): RandomAccessible<BoolType> {
    return this ge that.asType()
}

infix fun Number.ge(that: RandomAccessible<out RealType<*>>): RandomAccessible<BoolType> {
    return that le this
}
