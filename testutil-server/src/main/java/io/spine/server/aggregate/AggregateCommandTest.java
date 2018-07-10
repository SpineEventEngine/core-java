/*
 * Copyright 2018, TeamDev. All rights reserved.
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
package io.spine.server.aggregate;

import com.google.protobuf.Message;
import io.spine.server.CommandHandlerTest;

import java.util.List;

import static io.spine.core.CommandEnvelope.of;
import static io.spine.server.aggregate.AggregateMessageDispatcher.dispatchCommand;

/**
 * The implementation base for testing a single command handling in an {@link Aggregate}.
 *
 * @param <I> ID message of the aggregate
 * @param <C> type of the command to test
 * @param <S> the aggregate state type
 * @param <A> the {@link Aggregate} type
 * @author Vladyslav Lubenskyi
 */
public abstract class AggregateCommandTest<I,
                                           C extends Message,
                                           S extends Message,
                                           A extends Aggregate<I, S, ?>>
        extends CommandHandlerTest<I, C, S, A> {

    @Override
    protected List<? extends Message> dispatchTo(A entity) {
        return dispatchCommand(entity, of(createCommand(message())));
    }
}
