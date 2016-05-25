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
import org.spine3.base.Event;
import org.spine3.base.Response;
import org.spine3.client.grpc.Topic;
import org.spine3.server.command.error.CommandException;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.server.type.CommandClass;

import java.util.Set;

/**
 * The {@code ClientService} allows client applications to post commands and
 * receive updates from the application backend.
 *
 * @author Alexander Yevsyukov
 */
public class ClientService implements org.spine3.client.grpc.ClientServiceGrpc.ClientService {

    private final ImmutableMap<CommandClass, BoundedContext> commandToBoundedContext;

    protected ClientService(Builder builder) {
        this.commandToBoundedContext = builder.commandToBoundedContextMap().build();
    }

    @Override
    public void post(Command request, StreamObserver<Response> responseObserver) {
        final CommandClass commandClass = CommandClass.of(request);
        final BoundedContext boundedContext = commandToBoundedContext.get(commandClass);
        if (boundedContext == null) {
            handleUnsupported(request, responseObserver);
            return;
        }
        boundedContext.post(request, responseObserver);
    }

    private static void handleUnsupported(Command request, StreamObserver<Response> responseObserver) {
        final CommandException unsupported = new UnsupportedCommandException(request);
        log().error("Unsupported command posted to ClientService", unsupported);
        responseObserver.onError(Statuses.invalidArgumentWithCause(unsupported));
    }

    @Override
    public void subscribe(Topic request, StreamObserver<Event> responseObserver) {
        //TODO:2016-05-25:alexander.yevsyukov: Subscribe the client to the topic in the corresponding BoundedContext.
    }

    @Override
    public void unsubscribe(Topic request, StreamObserver<Response> responseObserver) {
        //TODO:2016-05-25:alexander.yevsyukov: Unsubscribe the client from the topic in the corresponding BoundedContext.
    }

    public static class Builder {
        private final Set<BoundedContext> boundedContexts = Sets.newHashSet();

        public Builder addBoundedContext(BoundedContext boundedContext) {
            boundedContexts.add(boundedContext);
            return this;
        }

        public Builder removeBoundedContext(BoundedContext boundedContext) {
            boundedContexts.remove(boundedContext);
            return this;
        }

        public ClientService build() {
            final ClientService result = new ClientService(this);
            return result;
        }

        private ImmutableMap.Builder<CommandClass, BoundedContext> commandToBoundedContextMap() {
            final ImmutableMap.Builder<CommandClass, BoundedContext> mapBuilder = ImmutableMap.builder();
            for (BoundedContext boundedContext : boundedContexts) {
                addBoundedContext(mapBuilder, boundedContext);
            }
            return mapBuilder;
        }

        private static void addBoundedContext(ImmutableMap.Builder<CommandClass, BoundedContext> mapBuilder,
                                              BoundedContext boundedContext) {
            for (CommandClass commandClass : boundedContext.getCommandBus().getSupportedCommandClasses()) {
                mapBuilder.put(commandClass, boundedContext);
            }
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ClientService.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
