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

package io.spine.server.delivery;

import io.spine.server.commandbus.DuplicateCommandException;
import io.spine.server.type.CommandEnvelope;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * The part of {@link Inbox} responsible for processing incoming
 * {@link io.spine.server.type.CommandEnvelope commands}.
 *
 * @param <I>
 *         the type of identifier or inbox target entities
 */
class InboxOfCommands<I> extends InboxPart<I, CommandEnvelope> {

    InboxOfCommands(Inbox.Builder<I> builder) {
        super(builder, builder.getCommandEndpoints());
    }

    @Override
    protected void setRecordPayload(CommandEnvelope envelope, InboxMessage.Builder builder) {
        builder.setCommand(envelope.outerObject());
    }

    @Override
    protected InboxMessageId inboxMsgIdFrom(CommandEnvelope envelope) {
        String rawValue = envelope.id()
                                  .getUuid();
        InboxMessageId result = InboxMessageId.newBuilder()
                                              .setValue(rawValue)
                                              .build();
        return result;
    }

    @Override
    protected CommandEnvelope asEnvelope(InboxMessage message) {
        return CommandEnvelope.of(message.getCommand());
    }

    @Override
    protected Dispatcher dispatcherWith(Collection<InboxMessage> deduplicationSource) {
        return new CommandDispatcher(deduplicationSource);
    }

    /**
     * A strategy of command delivery from this {@code Inbox} to the command target.
     */
    class CommandDispatcher extends Dispatcher {

        private CommandDispatcher(Collection<InboxMessage> deduplicationSource) {
            super(deduplicationSource);
        }

        @Override
        protected Predicate<? super InboxMessage> filterByType() {
            return (Predicate<InboxMessage>) InboxMessage::hasCommand;
        }

        @Override
        protected RuntimeException onDuplicateFound(InboxMessage duplicate) {
            return DuplicateCommandException.of(duplicate.getCommand());
        }
    }
}
