/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.commandbus;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.grpc.stub.StreamObserver;
import io.spine.core.CommandEnvelope;
import io.spine.core.IsSent;

import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * The chain of {@code CommandBusFilter}s that {@link CommandBus} applies before
 * {@linkplain CommandBus#post(com.google.protobuf.Message, StreamObserver) posting} a command.
 *
 * @author Alexander Yevsyukov
 */
final class FilterChain implements CommandBusFilter {

    private final ImmutableList<CommandBusFilter> filters;

    private FilterChain(ImmutableList<CommandBusFilter> filters) {
        this.filters = filters;
    }

    static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public Optional<IsSent> accept(CommandEnvelope envelope) {
        for (CommandBusFilter filter : filters) {
            final Optional<IsSent> result = filter.accept(envelope);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.absent();
    }

    @Override
    public void onClose(CommandBus commandBus) {
        for (CommandBusFilter filter : filters.reverse()) {
            filter.onClose(commandBus);
        }
    }

    static class Builder {

        private final List<CommandBusFilter> filters = Lists.newArrayList();

        private CommandBus commandBus;
        private CommandScheduler commandScheduler;

        Builder setCommandBus(CommandBus commandBus) {
            this.commandBus = commandBus;
            return this;
        }

        Builder setCommandScheduler(CommandScheduler commandScheduler) {
            this.commandScheduler = commandScheduler;
            return this;
        }

        Builder addFilters(List<CommandBusFilter> filters) {
            this.filters.addAll(filters);
            return this;
        }

        FilterChain build() {
            checkState(commandBus != null, "CommandBus must be set");
            checkState(commandScheduler != null, "CommandScheduler must be set");

            filters.add(0, new DeadCommandFilter(commandBus));
            filters.add(1, new ValidationFilter(commandBus));
            filters.add(2, commandScheduler);

            final FilterChain result = new FilterChain(ImmutableList.copyOf(filters));
            return result;
        }
    }
}
