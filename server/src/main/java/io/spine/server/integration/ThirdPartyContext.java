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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.BoundedContextBuilder.notStoringEvents;
import static io.spine.util.Preconditions2.checkNotEmptyOrBlank;
import static io.spine.validate.Validate.checkNotDefault;
import static io.spine.validate.Validate.checkValid;

/**
 * An external non-Spine based upstream system.
 *
 * <p>{@code ThirdPartyContext} helps to represent an external system as a Bounded Context. Events
 * which occur in the external system are converted into domain events of the user's
 * Bounded Contexts and dispatched via {@link IntegrationBus}.
 *
 * @implSpec Note that a {@code ThirdPartyContext} sends a request for external messages to
 *         other contexts. The {@code ThirdPartyContext} never consumes external messages itself,
 *         but requires the other Bounded Contexts to send their requests, so that the publishing
 *         channels are open. Depending of the implementation of
 *         {@link io.spine.server.transport.TransportFactory transport}, creating
 *         a {@code ThirdPartyContext} may be an expensive operation. Thus, it is recommended that
 *         the instances of this class are reused and {@linkplain #close() closed} when they are
 *         no longer needed.
 */
public final class ThirdPartyContext implements AutoCloseable {

    private final BoundedContext context;
    private final Any producerId;

    /**
     * Creates a new single-tenant instance of {@code ThirdPartyContext} with the given name.
     *
     * @param name
     *         name of the Bounded Context representing a part of a third-party system
     */
    public static ThirdPartyContext singleTenant(String name) {
        return newContext(name, false);
    }

    /**
     * Creates a new multitenant instance of {@code ThirdPartyContext} with the given name.
     *
     * @param name
     *         name of the Bounded Context representing a part of a third-party system
     */
    public static ThirdPartyContext multitenant(String name) {
        return newContext(name, true);
    }

    private static ThirdPartyContext newContext(String name, boolean multitenant) {
        checkNotEmptyOrBlank(name);
        BoundedContext context = notStoringEvents(name, multitenant).build();
        context.integrationBus()
               .notifyOfCurrentNeeds();
        return new ThirdPartyContext(context);
    }

    private ThirdPartyContext(BoundedContext context) {
        this.context = context;
        this.producerId = pack(context.name());
    }

    /**
     * Emits an event from the third-party system.
     *
     * <p>If the event is required by another Context, posts the event into
     * the {@link IntegrationBus} of the respective Context. Does nothing if the event is not
     * required by any Context.
     *
     * <p>The caller is required to supply the tenant ID via the {@code ActorContext.tenant_id} if
     * this Context is multitenant.
     *
     * @param actorContext
     *         the info about the actor, a user or a software component, who emits the event
     * @param eventMessage
     *         the event
     */
    public void emittedEvent(ActorContext actorContext, EventMessage eventMessage) {
        checkNotNull(actorContext);
        checkNotNull(eventMessage);
        checkValid(actorContext);
        checkNotDefault(eventMessage);
        checkValid(eventMessage);
        checkTenant(actorContext, eventMessage);

        EventId id = EventId
                .newBuilder()
                .setValue(newUuid())
                .build();
        EventContext eventContext = EventContext
                .newBuilder()
                .setProducerId(producerId)
                .setTimestamp(actorContext.getTimestamp())
                .setImportContext(actorContext)
                .setExternal(true)
                .vBuild();
        Event event = Event
                .newBuilder()
                .setId(id)
                .setContext(eventContext)
                .setMessage(pack(eventMessage))
                .vBuild();
        context.eventBus()
               .post(event);
    }

    /**
     * Emits an event from the third-party system.
     *
     * <p>If the event is required by another Context, posts the event into
     * the {@link IntegrationBus} of the respective Context. Does nothing if the event is not
     * required by any Context.
     *
     * <p>This overload may only be used for single-tenant third-party contexts. If this Context is
     * multitenant, this method throws an exception.
     *
     * @param userId
     *         the ID of the actor, a user or a software component, who emits the event
     * @param eventMessage
     *         the event
     */
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
        boolean tenantSupplied = actorContext.hasTenantId();
        if (context.isMultitenant()) {
            checkArgument(tenantSupplied,
                          "Cannot post `%s` into a third-party multitenant context %s." +
                                  " No tenant ID supplied.",
                          event.getClass().getSimpleName(),
                          context.name().getValue());
        } else {
            checkArgument(!tenantSupplied,
                          "Cannot post `%s` into a third-party single-tenant context %s." +
                                  " Tenant ID must NOT be supplied.",
                          event.getClass().getSimpleName(),
                          context.name().getValue());
        }
    }

    /**
     * Closes this Context and clean up underlying resources.
     *
     * <p>Attempts of emitting an event from a closed Context result in an exception.
     *
     * @throws Exception
     *         if the underlying {@link BoundedContext} fails to close
     */
    @Override
    public void close() throws Exception {
        context.close();
    }
}
