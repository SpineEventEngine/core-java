/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import io.spine.core.Command;
import io.spine.server.type.CommandEnvelope;

/**
 * The part of {@link Inbox} responsible for processing incoming
 * {@link io.spine.server.type.CommandEnvelope commands}.
 *
 * @param <I>
 *         the type of identifier or inbox target entities
 */
final class InboxOfCommands<I> extends InboxPart<I, CommandEnvelope> {

    InboxOfCommands(Inbox.Builder<I> builder) {
        super(builder, builder.commandEndpoints());
    }

    @Override
    protected void setRecordPayload(CommandEnvelope envelope, InboxMessage.Builder builder) {
        Command command = envelope.outerObject();
        builder.setCommand(command);
    }

    @Override
    protected String extractUuidFrom(CommandEnvelope envelope) {
        return envelope.id()
                       .getUuid();
    }

    @Override
    protected CommandEnvelope asEnvelope(InboxMessage message) {
        return CommandEnvelope.of(message.getCommand());
    }

    @Override
    protected InboxMessageStatus determineStatus(CommandEnvelope message,
                                                 InboxLabel label) {
        if(message.context().hasSchedule()) {
            return InboxMessageStatus.SCHEDULED;
        }
        return super.determineStatus(message, label);
    }
}
