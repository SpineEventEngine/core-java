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

import com.google.common.base.Throwables
import io.spine.base.RejectionThrowable
import io.spine.core.Event
import io.spine.core.RejectionEventContext
import io.spine.server.type.CommandEnvelope
import io.spine.server.type.RejectionEnvelope.PRODUCER_UNKNOWN

/**
 * A factory for producing rejection events.
 */
internal class RejectionFactory(
    val origin: CommandEnvelope,
    val throwable: RejectionThrowable
) : EventFactory(
    EventOrigin.fromAnotherMessage(origin),
    throwable.producerId().orElse(PRODUCER_UNKNOWN)
) {

    /**
     * Creates a rejection event which does not have version information.
     */
    fun createRejection(): Event {
        val msg = throwable.messageThrown()
        val context = rejectionContext()
        return createRejectionEvent(msg, null, context)
    }

    /**
     * Constructs a new [RejectionEventContext].
     */
    private fun rejectionContext(): RejectionEventContext {
        val command = origin.outerObject()
        val stacktrace = Throwables.getStackTraceAsString(throwable)
        return RejectionEventContext.newBuilder()
            .setCommand(command)
            .setStacktrace(stacktrace)
            .build()
    }
}
