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

package io.spine.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import io.grpc.stub.StreamObserver;
import io.spine.base.Error;
import io.spine.client.grpc.CommandServiceGrpc;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandClass;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.UnsupportedCommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static io.spine.server.bus.Buses.reject;

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
     * Constructs new instance using the map from a {@code CommandClass} to
     * a {@code BoundedContext} instance which handles the command.
     */
    protected CommandService(Map<CommandClass, BoundedContext> map) {
        super();
        this.boundedContextMap = ImmutableMap.copyOf(map);
    }

    @Override
    public void post(Command request, StreamObserver<Ack> responseObserver) {
        CommandClass commandClass = CommandClass.of(request);
        BoundedContext boundedContext = boundedContextMap.get(commandClass);
        if (boundedContext == null) {
            handleUnsupported(request, responseObserver);
        } else {
            CommandBus commandBus = boundedContext.getCommandBus();
            commandBus.post(request, responseObserver);
        }
    }

    private static void handleUnsupported(Command request,
                                          StreamObserver<Ack> responseObserver) {
        UnsupportedCommandException unsupported = new UnsupportedCommandException(request);
        log().error("Unsupported command posted to CommandService", unsupported);
        Error error = unsupported.asError();
        Ack response = reject(request.getId(), error);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @CanIgnoreReturnValue
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
        @CheckReturnValue
        public boolean contains(BoundedContext boundedContext) {
            boolean contains = boundedContexts.contains(boundedContext);
            return contains;
        }

        /**
         * Builds a new {@link CommandService}.
         */
        @CheckReturnValue
        public CommandService build() {
            ImmutableMap<CommandClass, BoundedContext> map = createMap();
            CommandService result = new CommandService(map);
            return result;
        }

        /**
         * Creates a map from {@code CommandClass}es to {@code BoundedContext}s that
         * handle such commands.
         */
        @CheckReturnValue
        private ImmutableMap<CommandClass, BoundedContext> createMap() {
            ImmutableMap.Builder<CommandClass, BoundedContext> builder = ImmutableMap.builder();
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
                                       ImmutableMap.Builder<CommandClass, BoundedContext> builder) {
            CommandBus commandBus = boundedContext.getCommandBus();
            Set<CommandClass> cmdClasses = commandBus.getRegisteredCommandClasses();
            for (CommandClass commandClass : cmdClasses) {
                builder.put(commandClass, boundedContext);
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
