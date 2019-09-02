/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.integration;

import com.google.protobuf.Any;
import io.spine.base.EventMessage;
import io.spine.core.ActorContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.protobuf.AnyPacker.pack;

/**
 * An external upstream system which is not implemented in Spine.
 *
 * <p>{@code ThirdPartyContext} helps to represent an upstream system as a Bounded Context. Events
 * in the external system are converted into domain events of the user's Bounded Contexts and
 * dispatched via {@link IntegrationBus}.
 */
public final class ThirdPartyContext implements AutoCloseable {

    private final BoundedContext context;
    private final Any producerId;

    public static ThirdPartyContext singleTenant(String name) {
        checkNotNull(name);
        BoundedContext context = BoundedContext.singleTenant(name).build();
        return new ThirdPartyContext(context);
    }

    public static ThirdPartyContext multitenant(String name) {
        checkNotNull(name);
        BoundedContext context = BoundedContext.multitenant(name).build();
        return new ThirdPartyContext(context);
    }

    private ThirdPartyContext(BoundedContext context) {
        this.context = context;
        this.producerId = pack(context.name());
        context.integrationBus()
               .notifyOfCurrentNeeds();
    }

    public void emittedEvent(ActorContext actorContext, EventMessage eventMessage) {
        checkNotNull(actorContext);
        checkNotNull(eventMessage);
        checkTenant(actorContext, eventMessage);

        EventContext eventContext = EventContext
                .newBuilder()
                .setProducerId(producerId)
                .setTimestamp(actorContext.getTimestamp())
                .setImportContext(actorContext)
                .setExternal(true)
                .vBuild();
        EventId id = EventId
                .newBuilder()
                .setValue(newUuid())
                .build();
        Event event = Event
                .newBuilder()
                .setId(id)
                .setContext(eventContext)
                .setMessage(pack(eventMessage))
                .vBuild();
        context.eventBus()
               .post(event);
    }

    public void emittedEvent(UserId userId, EventMessage eventMessage) {
        checkNotNull(userId);
        checkNotNull(eventMessage);
        ActorContext context = ActorContext
                .newBuilder()
                .setActor(userId)
                .setTimestamp(currentTime())
                .vBuild();
        emittedEvent(context, eventMessage);
    }

    private void checkTenant(ActorContext actorContext, EventMessage event) {
        if (context.isMultitenant()) {
            checkState(actorContext.hasActor(),
                       "Cannot post `%s` into a third-party multitenant context %s." +
                               " No tenant ID supplied." +
                               " Use `emittedEvent(ActorContext, EventMessage)` instead.",
                       event.getClass().getSimpleName(),
                       context.name().getValue());
        }
    }

    @Override
    public void close() throws Exception {
        context.close();
    }
}
