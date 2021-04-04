/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import com.google.common.base.Preconditions
import com.google.protobuf.Any
import com.google.protobuf.Message
import io.spine.base.EventMessage
import io.spine.core.Event
import io.spine.core.EventContext
import io.spine.core.Events
import io.spine.core.Version
import io.spine.protobuf.AnyPacker
import io.spine.protobuf.AnyPacker.pack
import io.spine.type.TypeName
import io.spine.validate.Validate
import io.spine.validate.ValidationException


abstract class EventFactoryBase(
    val origin: EventOrigin,
    val producerId: Any
) {
    /**
     * Creates a new event context with an optionally passed version of the entity
     * which produced the event.
     */
    fun createContext(version: Version?): EventContext =
        newContext(version).vBuild()

    protected fun newContext(version: Version?): EventContext.Builder {
        val builder = origin.contextBuilder().setProducerId(producerId)
        if (version != null) {
            builder.version = version
        }
        return builder
    }

    internal companion object {

        /**
         * Creates a new `Event` instance.
         */
        @JvmStatic
        fun assemble(message: EventMessage, context: EventContext): Event {
            // We validate now, before emitting the next ID.
            validate(message)
            val eventId = Events.generateId()
            val packed = pack(message)
            return with(Event.newBuilder()) {
                id = eventId
                this.message = packed
                this.context = context
                vBuild()
            }
        }

        /**
         * Validates an event message according to their Protobuf definition.
         *
         * If the given `messageOrAny` is an instance of `Any`, it is unpacked
         * for the validation.
         */
        @JvmStatic
        @Throws(ValidationException::class)
        private fun validate(messageOrAny: Message) {
            val message = if (messageOrAny is Any) {
                AnyPacker.unpack(messageOrAny)
            } else {
                messageOrAny
            }
            Preconditions.checkArgument(
                messageOrAny is EventMessage,
                "`%s` is not an event type.", TypeName.of(messageOrAny)
            )
            Validate.checkValid(message)
        }
    }
}
