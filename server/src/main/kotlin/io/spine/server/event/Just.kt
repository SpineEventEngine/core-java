/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.event

import io.spine.base.EventMessage
import io.spine.server.model.Nothing
import io.spine.server.tuple.Tuple

/**
 * A tuple of one event.
 *
 * Used when returning an `Iterable` from a handler method for better readability over
 * `Iterable<E>` or `List<E>`.
 *
 * @param E the type of the event.
 */
public class Just<E : EventMessage>(event: E) : Tuple(event) {

    public companion object {

        @Suppress("ConstPropertyName") // Following Java conventions.
        private const val serialVersionUID: Long = 0L

        /**
         * The instance of `Just<Nothing>`.
         */
        public val nothing: Just<Nothing> by lazy {
            Just(Nothing.getDefaultInstance())
        }

        /**
         * A factory method for Java.
         *
         * Prefer the primary constructor in Kotlin.
         *
         * This method is intended to be imported statically.
         */
        @JvmStatic
        public fun <E : EventMessage> just(event: E): Just<E> = Just(event)

        /**
         * Obtains the instance of `Just<Noting>` for Java code.
         *
         * Prefer the [nothing] property of the companion object in Kotlin.
         */
        @JvmStatic
        public fun nothing(): Just<Nothing> = nothing
    }
}
