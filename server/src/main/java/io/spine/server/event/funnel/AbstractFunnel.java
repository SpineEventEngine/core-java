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

package io.spine.server.event.funnel;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.spine.base.EventMessage;
import io.spine.base.Time;
import io.spine.core.Ack;
import io.spine.core.ActorContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.bus.Bus;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Identifier.newUuid;
import static java.util.stream.Collectors.toList;

abstract class AbstractFunnel<M extends Message> implements EventFunnel {

    private final Bus<M, ?, ?, ?> bus;
    private final UserId actor;
    private final Any producerId;
    private final StreamObserver<Ack> resultObserver;
    private final boolean fromExternalSource;
    private final @Nullable TenantId tenantId;

    AbstractFunnel(@Nullable TenantId tenantId,
                   Bus<M, ?, ?, ?> bus,
                   StreamObserver<Ack> resultObserver,
                   UserId actor,
                   boolean fromExternalSource) {
        this.tenantId = tenantId;
        this.bus = checkNotNull(bus);
        this.actor = checkNotNull(actor);
        this.resultObserver = checkNotNull(resultObserver);
        this.producerId = AnyPacker.pack(actor);
        this.fromExternalSource = fromExternalSource;
    }

    abstract M transformEvent(Event event);

    abstract AbstractFunnel<M> copyWithObserver(StreamObserver<Ack> resultObserver);

    @Override
    public final EventFunnel with(StreamObserver<Ack> resultObserver) {
        checkNotNull(resultObserver);
        return resultObserver.equals(this.resultObserver)
               ? this
               : copyWithObserver(resultObserver);
    }

    @Override
    public final void post(EventMessage... events) {
        checkNotNull(events);
        Timestamp time = Time.currentTime();
        ActorContext importContext = buildImportContext(time);
        Iterable<M> messages = Stream
                .of(events)
                .map((EventMessage message) -> buildEvent(message, importContext))
                .map(this::transformEvent)
                .collect(toList());
        bus.post(messages, resultObserver());
    }

    private Event buildEvent(EventMessage message, ActorContext importContext) {
        EventId id = buildEventId();
        EventContext context = buildContext(importContext);
        Event event = Event
                .newBuilder()
                .setId(id)
                .setMessage(AnyPacker.pack(message))
                .setContext(context)
                .vBuild();
        return event;
    }

    private static EventId buildEventId() {
        return EventId
                .newBuilder()
                .setValue(newUuid())
                .build();
    }

    private EventContext buildContext(ActorContext importContext) {
        return EventContext
                .newBuilder()
                .setTimestamp(importContext.getTimestamp())
                .setProducerId(producerId)
                .setImportContext(importContext)
                .setExternal(fromExternalSource)
                .build();
    }

    private ActorContext buildImportContext(Timestamp time) {
        ActorContext.Builder context = ActorContext
                .newBuilder()
                .setActor(actor)
                .setTimestamp(time);
        if (tenantId != null) {
            context.setTenantId(tenantId);
        }
        return context.vBuild();
    }

    final StreamObserver<Ack> resultObserver() {
        return resultObserver;
    }

    final UserId actor() {
        return actor;
    }

    final TenantId tenantId() {
        return tenantId;
    }
}
