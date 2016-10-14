/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Command;
import org.spine3.base.Response;
import org.spine3.client.grpc.CommandServiceGrpc;
import org.spine3.server.command.error.CommandException;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.server.type.CommandClass;

import java.util.Set;

/**
 * The {@code CommandService} allows client applications to post commands and
 * receive updates from the application backend.
 *
 * @author Alexander Yevsyukov
 */
public class CommandService
        extends CommandServiceGrpc.CommandServiceImplBase {

    private final ImmutableMap<CommandClass, BoundedContext> boundedContextMap;

    public static Builder newBuilder() {
        return new Builder();
    }

    protected CommandService(Builder builder) {
        this.boundedContextMap = builder.getBoundedContextMap();
    }

    @SuppressWarnings("RefusedBequest") // as we override default implementation with `unimplemented` status.
    @Override
    public void post(Command request, StreamObserver<Response> responseObserver) {
        final CommandClass commandClass = CommandClass.of(request);
        final BoundedContext boundedContext = boundedContextMap.get(commandClass);
        if (boundedContext == null) {
            handleUnsupported(request, responseObserver);
        } else {
            boundedContext.getCommandBus()
                          .post(request, responseObserver);
        }
    }

    private static void handleUnsupported(Command request, StreamObserver<Response> responseObserver) {
        final CommandException unsupported = new UnsupportedCommandException(request);
        log().error("Unsupported command posted to CommandService", unsupported);
        responseObserver.onError(Statuses.invalidArgumentWithCause(unsupported));
    }

    public static class Builder {

        private final Set<BoundedContext> boundedContexts = Sets.newHashSet();
        private ImmutableMap<CommandClass, BoundedContext> boundedContextMap;

        public Builder addBoundedContext(BoundedContext boundedContext) {
            // Save it to a temporary set so that it is easy to remove it if needed.
            boundedContexts.add(boundedContext);
            return this;
        }

        public Builder removeBoundedContext(BoundedContext boundedContext) {
            boundedContexts.remove(boundedContext);
            return this;
        }

        @SuppressWarnings("ReturnOfCollectionOrArrayField") // is immutable
        public ImmutableMap<CommandClass, BoundedContext> getBoundedContextMap() {
            return boundedContextMap;
        }

        /**
         * Builds the {@link CommandService}.
         */
        public CommandService build() {
            this.boundedContextMap = createBoundedContextMap();
            final CommandService result = new CommandService(this);
            return result;
        }

        private ImmutableMap<CommandClass, BoundedContext> createBoundedContextMap() {
            final ImmutableMap.Builder<CommandClass, BoundedContext> builder = ImmutableMap.builder();
            for (BoundedContext boundedContext : boundedContexts) {
                addBoundedContext(builder, boundedContext);
            }
            return builder.build();
        }

        private static void addBoundedContext(ImmutableMap.Builder<CommandClass, BoundedContext> mapBuilder,
                BoundedContext boundedContext) {
            final Set<CommandClass> cmdClasses = boundedContext.getCommandBus()
                                                               .getSupportedCommandClasses();
            for (CommandClass commandClass : cmdClasses) {
                mapBuilder.put(commandClass, boundedContext);
            }
        }
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(CommandService.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
