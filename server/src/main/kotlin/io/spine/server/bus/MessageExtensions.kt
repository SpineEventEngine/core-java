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

package io.spine.server.bus

import com.google.protobuf.Message
import io.spine.base.Error
import io.spine.core.Ack
import io.spine.core.CommandId
import io.spine.core.Event
import io.spine.core.Responses
import io.spine.core.Status
import io.spine.protobuf.AnyPacker

/**
 * Acknowledges this message with the OK status.
 */
fun Message.acknowledge(): Ack =
    ackWithStatus(this, Responses.statusOk())

/**
 * Rejects message with this ID because the passed error occurred.
 */
fun Message.causedError(cause: Error): Ack =
    ackWithStatus(this, Responses.errorWith(cause))

/**
 * Reject a command with this ID with the passed rejection event.
 */
fun CommandId.reject(rejection: Event): Ack =
    ackWithStatus(this, Responses.rejectedBecauseOf(rejection))

private fun ackWithStatus(id: Message, status: Status): Ack {
    val packedId = AnyPacker.pack(id)
    return with(Ack.newBuilder()) {
        messageId = packedId
        this.status = status
        vBuild()
    }
}
