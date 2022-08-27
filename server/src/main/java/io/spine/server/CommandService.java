/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.grpc.stub.StreamObserver;
import io.spine.base.Error;
import io.spine.base.Errors;
import io.spine.client.grpc.CommandServiceGrpc;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.logging.Logging;
import io.spine.server.commandbus.UnsupportedCommandException;
import io.spine.server.type.CommandClass;
import io.spine.type.UnpublishedLanguageException;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.bus.MessageIdExtensions.causedError;
import static io.spine.type.MessageExtensions.isInternal;

/**
 * The {@code CommandService} allows client applications to post commands and
 * receive updates from the application backend.
 */
public final class CommandService
        extends CommandServiceGrpc.CommandServiceImplBase
        implements Logging {

    private final ImmutableMap<CommandClass, BoundedContext> commandToContext;

    /**
     * Constructs new instance using the map from a {@code CommandClass} to
     * a {@code BoundedContext} instance which handles the command.
     */
    private CommandService(Map<CommandClass, BoundedContext> map) {
        super();
        this.commandToContext = ImmutableMap.copyOf(map);
        if (map.isEmpty()) {
            /* We do not prohibit such a case of "empty" service for unusual cases of serving
               no commands (e.g. because of handling only events) or for creating stub instances. */
            _warn().log("A `%s` with no bounded contexts has been created.", simpleClassName());
        }
    }

    private String simpleClassName() {
        return getClass().getSimpleName();
    }

    /**
     * Creates a new builder for the service.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builds the service with a single Bounded Context.
     */
    public static CommandService withSingle(BoundedContext context) {
        checkNotNull(context);
        var result = newBuilder().add(context).build();
        return result;
    }

    @Override
    public void post(Command request, StreamObserver<Ack> responseObserver) {
        var commandClass = CommandClass.of(request);
        if (isInternal(commandClass.value())) {
            handleInternal(request, responseObserver);
            return;
        }
        var boundedContext = commandToContext.get(commandClass);
        if (boundedContext == null) {
            handleUnsupported(request, responseObserver);
        } else {
            var commandBus = boundedContext.commandBus();
            commandBus.post(request, responseObserver);
        }
    }

    private void handleInternal(Command command, StreamObserver<Ack> responseObserver) {
        var unpublishedLanguage = new UnpublishedLanguageException(command.enclosedMessage());
        _error().withCause(unpublishedLanguage)
                .log("Unpublished command posted to `%s`.", simpleClassName());
        var error = Errors.fromThrowable(unpublishedLanguage);
        respondWithError(command, error, responseObserver);
    }

    private void handleUnsupported(Command command, StreamObserver<Ack> responseObserver) {
        var unsupported = new UnsupportedCommandException(command);
        _error().withCause(unsupported)
                .log("Unsupported command posted to `%s`.", simpleClassName());
        var error = unsupported.asError();
        respondWithError(command, error, responseObserver);
    }

    private static
    void respondWithError(Command command, Error error, StreamObserver<Ack> responseObserver) {
        var id = command.getId();
        var response = causedError(id, error);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * The builder for a {@code CommandService}.
     */
    public static class Builder extends AbstractServiceBuilder<CommandService, Builder> {

        /**
         * Builds a new {@link CommandService}.
         */
        @Override
        public CommandService build() {
            var map = createMap();
            var result = new CommandService(map);
            return result;
        }

        /**
         * Creates a map from {@code CommandClass}es to {@code BoundedContext}s that
         * handle such commands.
         */
        private ImmutableMap<CommandClass, BoundedContext> createMap() {
            ImmutableMap.Builder<CommandClass, BoundedContext> builder = ImmutableMap.builder();
            for (var boundedContext : contexts()) {
                putIntoMap(boundedContext, builder);
            }
            return builder.build();
        }

        /**
         * Associates {@code CommandClass}es with the instance of {@code BoundedContext}
         * that handles such commands.
         */
        private static void putIntoMap(BoundedContext context,
                                       ImmutableMap.Builder<CommandClass, BoundedContext> builder) {
            var commandBus = context.commandBus();
            var cmdClasses = commandBus.registeredCommandClasses();
            for (var commandClass : cmdClasses) {
                builder.put(commandClass, context);
            }
        }

        @Override
        Builder self() {
            return this;
        }
    }
}
