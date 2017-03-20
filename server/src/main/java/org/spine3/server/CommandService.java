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

package org.spine3.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Command;
import org.spine3.base.Response;
import org.spine3.client.grpc.CommandServiceGrpc;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandException;
import org.spine3.server.command.UnsupportedCommandException;
import org.spine3.type.CommandClass;

import java.util.Map;
import java.util.Set;

/**
 * The {@code CommandService} allows client applications to post commands and
 * receive updates from the application backend.
 *
 * @author Alexander Yevsyukov
 */
public class CommandService extends CommandServiceGrpc.CommandServiceImplBase {

    private final ImmutableMap<CommandClass, BoundedContext> boundedContextMap;

    /**
     * Creates a new builder for {@code CommandService}.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Constructs new instance using the map from a {@code CommandClass} to a {@code BoundedContext} instance
     * which handles the command.
     */
    protected CommandService(Map<CommandClass, BoundedContext> map) {
        super();
        this.boundedContextMap = ImmutableMap.copyOf(map);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
        // as we override default implementation with `unimplemented` status.
    @Override
    public void post(Command request, StreamObserver<Response> responseObserver) {
        final CommandClass commandClass = CommandClass.of(request);
        final BoundedContext boundedContext = boundedContextMap.get(commandClass);
        if (boundedContext == null) {
            handleUnsupported(request, responseObserver);
        } else {
            final CommandBus commandBus = boundedContext.getCommandBus();
            commandBus.post(request, responseObserver);
        }
    }

    private static void handleUnsupported(Command request, StreamObserver<Response> responseObserver) {
        final CommandException unsupported = new UnsupportedCommandException(request);
        log().error("Unsupported command posted to CommandService", unsupported);
        responseObserver.onError(Statuses.invalidArgumentWithCause(unsupported));
    }

    public static class Builder {

        private final Set<BoundedContext> boundedContexts = Sets.newHashSet();

        /**
         * Adds the {@code BoundedContext} to the builder.
         */
        public Builder add(BoundedContext boundedContext) {
            // Save it to a temporary set so that it is easy to remove it if needed.
            boundedContexts.add(boundedContext);
            return this;
        }

        /**
         * Removes the {@code BoundedContext} from the builder.
         */
        public Builder remove(BoundedContext boundedContext) {
            boundedContexts.remove(boundedContext);
            return this;
        }

        /**
         * Verifies if the passed {@code BoundedContext} was previously added to the builder.
         *
         * @param boundedContext the instance to check
         * @return {@code true} if the instance was added to the builder, {@code false} otherwise
         */
        public boolean contains(BoundedContext boundedContext) {
            final boolean contains = boundedContexts.contains(boundedContext);
            return contains;
        }

        /**
         * Builds a new {@link CommandService}.
         */
        public CommandService build() {
            final ImmutableMap<CommandClass, BoundedContext> map = createMap();
            final CommandService result = new CommandService(map);
            return result;
        }

        /**
         * Creates a map from {@code CommandClass}es to {@code BoundedContext}s that handle such commands.
         */
        private ImmutableMap<CommandClass, BoundedContext> createMap() {
            final ImmutableMap.Builder<CommandClass, BoundedContext> builder = ImmutableMap.builder();
            for (BoundedContext boundedContext : boundedContexts) {
                putIntoMap(boundedContext, builder);
            }
            return builder.build();
        }

        /**
         * Associates {@code CommandClass}es with the instance of {@code BoundedContext}
         * that handles such commands.
         */
        private static void putIntoMap(BoundedContext boundedContext,
                                       ImmutableMap.Builder<CommandClass, BoundedContext> mapBuilder) {
            final CommandBus commandBus = boundedContext.getCommandBus();
            final Set<CommandClass> cmdClasses = commandBus.getRegisteredCommandClasses();
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
