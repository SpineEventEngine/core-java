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

package io.spine.server.aggregate.given.repo;

import com.google.common.collect.ImmutableSet;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.core.MessageEnvelope;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.route.CommandRoute;
import io.spine.server.route.EventRoute;
import io.spine.test.aggregate.number.FloatEncountered;
import io.spine.test.aggregate.number.RejectNegativeInt;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

/**
 * The repository of {@link io.spine.server.aggregate.given.repo.FailingAggregate}s.
 */
public class FailingAggregateRepository
        extends AggregateRepository<Long, FailingAggregate> {

    private boolean errorLogged;
    private @Nullable MessageEnvelope lastErrorEnvelope;
    private @Nullable RuntimeException lastException;

    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    public FailingAggregateRepository() {
        super();
        getCommandRouting().replaceDefault(
                // Simplistic routing function that takes absolute value as ID.
                new CommandRoute<Long, CommandMessage>() {
                    private static final long serialVersionUID = 0L;

                    @Override
                    public Long apply(CommandMessage message, CommandContext context) {
                        if (message instanceof RejectNegativeInt) {
                            RejectNegativeInt event = (RejectNegativeInt) message;
                            return (long) Math.abs(event.getNumber());
                        }
                        return 0L;
                    }
                }
        );

        getEventRouting().replaceDefault(
                new EventRoute<Long, EventMessage>() {
                    private static final long serialVersionUID = 0L;

                    /**
                     * Returns several entity identifiers to check error isolation.
                     * @see io.spine.server.aggregate.given.repo.FailingAggregate#on(io.spine.test.aggregate.number.FloatEncountered)
                     */
                    @Override
                    public Set<Long> apply(EventMessage message, EventContext context) {
                        if (message instanceof FloatEncountered) {
                            long absValue = FailingAggregate.toId((FloatEncountered) message);
                            return ImmutableSet.of(absValue, absValue + 100, absValue + 200);
                        }
                        return ImmutableSet.of(1L, 2L);
                    }
                });
    }

    @Override
    protected void logError(String msgFormat,
                            MessageEnvelope envelope,
                            RuntimeException exception) {
        super.logError(msgFormat, envelope, exception);
        errorLogged = true;
        lastErrorEnvelope = envelope;
        lastException = exception;
    }

    public boolean isErrorLogged() {
        return errorLogged;
    }

    public @Nullable MessageEnvelope getLastErrorEnvelope() {
        return lastErrorEnvelope;
    }

    public @Nullable RuntimeException getLastException() {
        return lastException;
    }
}
