/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.tuple;

import com.google.protobuf.Message;

/**
 * A group of interfaces for obtaining tuple element values.
 *
 * @author Alexander Yevsyukov
 */
class Element {

    /**
     * Prevent instantiation of this utility class.
     */
    private Element() {}

    interface AValue<T extends Message> {
        /**
         * Obtains the first element of the tuple.
         */
        T getA();
    }

    /**
     * A marker interface for a tuple element which value can be
     * {@link com.google.common.base.Optional Optional}.
     */
    interface OptionalValue {}

    interface BValue<T> extends OptionalValue {
        /**
         * Obtains the second element of the tuple.
         */
        T getB();
    }

    interface CValue<T> extends OptionalValue {
        /**
         * Obtains the third element of the tuple.
         */
        T getC();
    }
}
