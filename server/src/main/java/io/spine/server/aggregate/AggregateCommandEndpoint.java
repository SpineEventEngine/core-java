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
import io.spine.annotation.Internal;
import io.spine.core.CommandEnvelope;

import java.util.List;

/**
 * Dispatches commands to aggregates of the associated {@code AggregateRepository}.
 *
 * @param <I> the type of the aggregate IDs
 * @param <A> the type of the aggregates managed by the parent repository
 * @author Alexander Yevsyukov
 */
@Internal
public class AggregateCommandEndpoint<I, A extends Aggregate<I, ?, ?>>
        extends AggregateEndpoint<I, A, CommandEnvelope, I> {

    protected AggregateCommandEndpoint(AggregateRepository<I, A> repo, CommandEnvelope command) {
        super(repo, command);
    }

    static <I, A extends Aggregate<I, ?, ?>>
    I handle(AggregateRepository<I, A> repository, CommandEnvelope command) {
        AggregateCommandEndpoint<I, A> endpoint = of(repository, command);

        return endpoint.handle();
    }

    static <I, A extends Aggregate<I, ?, ?>>
    AggregateCommandEndpoint<I, A>
    of(AggregateRepository<I, A> repository, CommandEnvelope command) {
        return new AggregateCommandEndpoint<>(repository, command);
    }

    @Override
    protected List<? extends Message> doDispatch(A aggregate, CommandEnvelope envelope) {
        repository().onDispatchCommand(aggregate.getId(), envelope.getCommand());
        return aggregate.dispatchCommand(envelope);
    }

    @Override
    protected AggregateDelivery<I, A, CommandEnvelope, ?, ?> getEndpointDelivery() {
        return repository().getCommandEndpointDelivery();
    }

    /**
     * Returns ID of the aggregate that is responsible for handling the command.
     */
    @Override
    protected I getTargets() {
        CommandEnvelope envelope = envelope();
        I id = repository().getCommandRouting()
                           .apply(envelope.getMessage(), envelope.getCommandContext());
        return id;
    }

    @Override
    protected void onError(CommandEnvelope envelope, RuntimeException exception) {
        repository().onError(envelope, exception);
    }

    /**
     * Throws {@link IllegalStateException} with the message containing details of the aggregate and
     * the command in response to which the aggregate generated empty set of event messages.
     *
     * @throws IllegalStateException always
     */
    @Override
    protected void onEmptyResult(A aggregate, CommandEnvelope cmd) throws IllegalStateException {
        String format =
                "The aggregate (class: %s, id: %s) produced empty response for " +
                "the command (class: %s, id: %s).";
        onUnhandledCommand(aggregate, cmd, format);
    }
}
