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

package io.spine.server.delivery;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.client.ActorRequestFactory;
import io.spine.core.ActorContext;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.delivery.event.CatchUpRequested;
import io.spine.server.event.EventFactory;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.tenant.TenantFunction;
import io.spine.server.type.EventClass;
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * The one who starts the catch-up process.
 *
 * <p>Checks whether the catch-up is already started for the requested IDs before emitting
 * {@linkplain CatchUpRequested an event} telling that the catch-up has been requested.
 */
final class CatchUpStarter<I> {

    private final BoundedContext context;
    private final TypeUrl projectionStateType;
    private final CatchUpStorage storage;
    private final ImmutableSet<EventClass> eventClasses;

    private CatchUpStarter(Builder<I> builder) {
        this.context = builder.context;
        this.projectionStateType = builder.projectionStateType;
        this.storage = builder.storage;
        this.eventClasses = builder.eventClasses;
    }

    static <I> Builder<I> newBuilder(ProjectionRepository<I, ?, ?> repo, CatchUpStorage storage) {
        return new Builder<>(repo.entityStateType(), repo.messageClasses(), storage);
    }

    /**
     * Starts the catch-up restricting it to the set of projection instances by certain identifiers.
     *
     * @param ids
     *         the IDs of the projection instances to catch-up, or {@code null} if all entities of
     *         this kind need to catch up.
     * @param since
     *         since when the catch-up is going to read the events
     * @throws CatchUpAlreadyStartedException
     *         if the catch-up is already in progress for at least one of the requested entities
     */
    public void start(@Nullable Set<I> ids, Timestamp since) throws CatchUpAlreadyStartedException {
        checkNotActive(ids);

        CatchUp.Request request = buildRequest(ids, since);
        CatchUpId id = CatchUpId.newBuilder()
                                .setUuid(Identifier.newUuid())
                                .setProjectionType(projectionStateType.value())
                                .vBuild();
        CatchUpRequested eventMessage = CatchUpRequested
                .newBuilder()
                .setId(id)
                .setRequest(request)
                .vBuild();
        EventFactory eventFactory = newEventFactory(projectionStateType, context.isMultitenant());
        Event event = eventFactory.createEvent(eventMessage, null);
        context.eventBus()
               .post(event);
    }

    private static EventFactory newEventFactory(TypeUrl stateType, boolean multitenant) {
        String userIdValue = format("`CatchUpStarter` for `%s`", stateType.value());
        UserId onBehalfOf = UserId.newBuilder()
                                  .setValue(userIdValue)
                                  .build();
        ActorRequestFactory requestFactory = requestFactory(onBehalfOf, multitenant);
        Any producerId = AnyPacker.pack(onBehalfOf);
        ActorContext actorContext = requestFactory.newActorContext();
        return EventFactory.forImport(actorContext, producerId);
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    private CatchUp.Request buildRequest(@Nullable Set<I> ids, Timestamp since) {
        CatchUp.Request.Builder requestBuilder = CatchUp.Request.newBuilder();
        if (ids != null) {
            for (I id : ids) {
                Any packed = Identifier.pack(id);
                requestBuilder.addTarget(packed);
            }
        }

        requestBuilder.setSinceWhen(since);
        for (EventClass eventClass : eventClasses) {
            TypeName name = eventClass.typeName();
            requestBuilder.addEventType(name.value());
        }
        return requestBuilder.vBuild();
    }

    private void checkNotActive(@Nullable Set<I> ids) throws CatchUpAlreadyStartedException {
        Iterable<CatchUp> ongoing = storage.readByType(projectionStateType);
        List<CatchUp> active = stream(ongoing)
                .filter(catchUp -> CatchUpStatus.COMPLETED != catchUp.getStatus())
                .collect(toList());
        boolean alreadyCatchingUp = hasIntersections(active, ids);
        if (alreadyCatchingUp) {
            throw new CatchUpAlreadyStartedException(projectionStateType, ids);
        }
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

    @SuppressWarnings("MethodWithMultipleLoops")
    private static boolean hasIntersections(Iterable<CatchUp> ongoing, @Nullable Set<?> ids) {
        if (ids == null) {
            return ongoing.iterator()
                          .hasNext();
        }
        if (ids.isEmpty()) {
            return false;
        }
        Set<Any> packedIds = ids.stream()
                                .map(Identifier::pack)
                                .collect(toSet());
        for (CatchUp ongoingProcess : ongoing) {
            List<Any> targets = ongoingProcess.getRequest()
                                              .getTargetList();
            if (targets.isEmpty()) {
                return true;
            }
            for (Any target : targets) {
                if (packedIds.contains(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    static final class Builder<I> {

        private final TypeUrl projectionStateType;
        private final CatchUpStorage storage;
        private final ImmutableSet<EventClass> eventClasses;

        private BoundedContext context;

        private Builder(TypeUrl stateType,
                        ImmutableSet<EventClass> consumedEvents,
                        CatchUpStorage storage) {
            this.projectionStateType = stateType;
            this.storage = storage;
            this.eventClasses = consumedEvents;
        }

        Builder<I> withContext(BoundedContext context) {
            this.context = checkNotNull(context);
            return this;
        }

        CatchUpStarter<I> build() {
            checkNotNull(context, "The Bounded Context must be set" +
                    "in order to create an instance of `CatchUpStarter`.");
            return new CatchUpStarter<>(this);
        }
    }
}