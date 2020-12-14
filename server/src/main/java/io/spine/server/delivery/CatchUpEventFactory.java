/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import com.google.protobuf.Any;
import io.spine.base.EventMessage;
import io.spine.client.ActorRequestFactory;
import io.spine.core.ActorContext;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.event.EventFactory;
import io.spine.server.tenant.TenantFunction;
import io.spine.type.TypeUrl;

import static java.lang.String.format;

/**
 * A factory of {@link Event}s emitted in the catch-up process.
 */
final class CatchUpEventFactory {

    private final EventFactory factory;

    CatchUpEventFactory(TypeUrl projectionStateType, boolean multitenant) {
        factory = newEventFactory(projectionStateType, multitenant);
    }

    /**
     * Produces a new {@code Event} from the passed event message.
     *
     * <p>If this method is executed in scope of a multi-tenant bounded context, the current
     * {@link TenantId} must be set somewhere up the caller stack. See {@link TenantFunction} for
     * one of the options on how to do that.
     */
    Event createEvent(EventMessage message) {
        return factory.createEvent(message, null);
    }

    private static EventFactory newEventFactory(TypeUrl stateType, boolean multitenant) {
        String userIdValue = format("`CatchUpEventFactory` for `%s`", stateType.value());
        UserId onBehalfOf = UserId.newBuilder()
                                  .setValue(userIdValue)
                                  .build();
        ActorRequestFactory requestFactory = requestFactory(onBehalfOf, multitenant);
        Any producerId = AnyPacker.pack(onBehalfOf);
        ActorContext actorContext = requestFactory.newActorContext();
        return EventFactory.forImport(actorContext, producerId);
    }

    private static ActorRequestFactory requestFactory(UserId actor, boolean multitenant) {
        TenantFunction<ActorRequestFactory> function =
                new TenantFunction<ActorRequestFactory>(multitenant) {
                    @Override
                    public ActorRequestFactory apply(TenantId id) {
                        return ActorRequestFactory.newBuilder()
                                                  .setActor(actor)
                                                  .setTenantId(id)
                                                  .build();
                    }
                };
        return function.execute();
    }
}
