/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.test.client;

import io.spine.base.CommandMessage;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Status;
import io.spine.core.UserId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.bus.BusFilter;
import io.spine.server.type.CommandEnvelope;
import io.spine.test.client.command.LogInUser;
import io.spine.testing.core.given.GivenUserId;

import java.util.Optional;

/**
 * Configures Bounded Context for the purpose of {@link io.spine.client.ClientTest}.
 */
public final class ClientTestContext {

    static final String NAME = "ClientTest";

    /**
     * The ID of the user which causes error when posting {@link LogInUser} command.
     *
     * @see #rejectingFilter()
     */
    public static final UserId INVALID_USER = GivenUserId.of("invalid-user");

    /** Prevents instantiation of this configuration class. */
    private ClientTestContext() {
    }

    public static BoundedContextBuilder builder() {
        return BoundedContext.singleTenant(NAME)
                             .addCommandFilter(rejectingFilter())
                             .add(LoginProcess.class)
                             .add(new ActiveUsersProjection.Repository());
    }

    /**
     * Creates a Command Bus filter which responds with an error when a command {@link LogInUser}
     * is posted with the {@link #INVALID_USER} argument.
     *
     * <p>This filter is a simulation of errors that may occur at the server side during
     * asynchronous posting of commands.
     */
    private static BusFilter<CommandEnvelope> rejectingFilter() {
        return envelope -> {
                CommandMessage commandMessage = envelope.message();
                if (commandMessage instanceof LogInUser) {
                    if (INVALID_USER.equals(((LogInUser)commandMessage).getUser())) {
                        Error error = Error
                                .newBuilder()
                                .setCode(-1)
                                .build();
                        Status status = Status
                                .newBuilder()
                                .setError(error)
                                .build();
                        return Optional.of(
                                Ack.newBuilder()
                                   .setMessageId(AnyPacker.pack(envelope.id()))
                                   .setStatus(status)
                                   .build()
                        );
                    }
                }
                return Optional.empty();
            };
    }
}
