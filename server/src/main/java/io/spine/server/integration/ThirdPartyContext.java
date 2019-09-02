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
import com.google.protobuf.Timestamp;
import io.spine.base.EventMessage;
import io.spine.base.Time;
import io.spine.core.ActorContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.time.ZoneId;
import io.spine.time.ZoneIds;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Identifier.newUuid;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.validate.Validate.isNotDefault;

public final class ThirdPartyContext implements AutoCloseable {

    private final BoundedContext context;
    private final TenantId tenantId;
    private final Any producerId;

    public ThirdPartyContext(String name, TenantId tenantId) {
        checkNotNull(name);
        this.tenantId = checkNotNull(tenantId);
        this.context = buildContext(name, tenantId);
        this.producerId = pack(context.name());
    }

    public ThirdPartyContext(String name) {
        this(name, TenantId.getDefaultInstance());
    }

    private static BoundedContext buildContext(String name, TenantId tenantId) {
        boolean multitenant = isNotDefault(tenantId);
        BoundedContextBuilder builder = multitenant
                                        ? BoundedContext.multitenant(name)
                                        : BoundedContext.singleTenant(name);
        BoundedContext context = builder.build();
        context.integrationBus()
               .notifyOfCurrentNeeds();
        return context;
    }

    public void emittedEvent(UserId actor, EventMessage eventMessage) {
        emittedEvent(actor, eventMessage, Time.currentTime(), ZoneIds.systemDefault());
    }

    public void emittedEvent(UserId actor,
                             EventMessage eventMessage,
                             Timestamp when,
                             ZoneId inZone) {
        checkNotNull(actor);
        checkNotNull(eventMessage);
        checkNotNull(when);
        checkNotNull(inZone);
        ActorContext actorContext = ActorContext
                .newBuilder()
                .setActor(actor)
                .setTenantId(tenantId)
                .setTimestamp(when)
                .setZoneId(inZone)
                .vBuild();
        post(actorContext, eventMessage);
    }

    private void post(ActorContext actorContext, EventMessage eventMessage) {
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

    @Override
    public void close() throws Exception {
        context.close();
    }
}
