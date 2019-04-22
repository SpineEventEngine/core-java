/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.inbox;

import io.spine.server.commandbus.DuplicateCommandException;
import io.spine.server.type.CommandEnvelope;

import java.util.Optional;

/**
 * The part of {@link Inbox} responsible for processing incoming
 * {@link io.spine.server.type.CommandEnvelope commands}.
 *
 * @param <I>
 *         the type of identifier or inbox target entities
 */
class InboxOfCommands<I> extends InboxPart<I, CommandEnvelope> {

    InboxOfCommands(CommandEnvelope envelope, Inbox.Builder<I> builder, I entityId) {
        super(envelope, builder, builder.getCommandEndpoints(), entityId);
    }

    @Override
    protected void setRecordPayload(CommandEnvelope envelope, InboxMessageVBuilder builder) {
        builder.setCommand(envelope.outerObject());
    }

    //TODO:2019-01-25:alex.tymchenko: should we post rejections?
    @Override
    protected Optional<DuplicateCommandException> checkDuplicates(InboxContentRecord contents) {
        CommandEnvelope envelope = getEnvelope();
        boolean hasDuplicate = contents.getMessageList()
                                       .stream()
                                       .filter(InboxMessage::hasCommand)
                                       .anyMatch(m -> envelope.id()
                                                              .equals(m.getCommand()
                                                                       .getId()));
        if (hasDuplicate) {
            DuplicateCommandException exception =
                    DuplicateCommandException.of(envelope.outerObject());
            return Optional.of(exception);
        }
        return Optional.empty();
    }
}
