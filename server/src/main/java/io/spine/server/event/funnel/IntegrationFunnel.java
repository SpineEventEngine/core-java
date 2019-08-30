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

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.spine.base.EventMessage;
import io.spine.base.Time;
import io.spine.core.Ack;
import io.spine.core.ActorContext;
import io.spine.core.UserId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessages;
import io.spine.server.integration.IntegrationBus;

import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.BoundedContextNames.outside;
import static java.util.stream.Collectors.toList;

final class IntegrationFunnel extends AbstractFunnel {

    private final IntegrationBus bus;

    IntegrationFunnel(IntegrationBus bus,
                      StreamObserver<Ack> resultObserver,
                      UserId actor) {
        super(actor, resultObserver);
        this.bus = checkNotNull(bus);
    }

    @Override
    AbstractFunnel copyWithObserver(StreamObserver<Ack> resultObserver) {
        return new IntegrationFunnel(bus, resultObserver(), actor());
    }

    @Override
    public void post(EventMessage... events) {
        checkNotNull(events);
        Timestamp time = Time.currentTime();
        ActorContext importContext = buildImportContext(time);
        Iterable<ExternalMessage> messages = Stream
                .of(events)
                .map((EventMessage message) -> buildEvent(message, importContext, true))
                .map(event -> ExternalMessages.of(event, outside()))
                .collect(toList());
        bus.post(messages, resultObserver());
    }
}
