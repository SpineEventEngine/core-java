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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import io.spine.logging.Logging;
import io.spine.server.event.EventDispatcher;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A subscriber to local {@code EventBus}, which publishes each matching domestic event to
 * a remote channel.
 *
 * <p>The events to subscribe are those that are required by external application components
 * at this moment; their set is determined by the {@linkplain RequestForExternalMessages
 * configuration messages}, received by this instance of {@code IntegrationBroker}.
 */
final class DomesticEventPublisher implements EventDispatcher, Logging {

    private final Set<EventClass> eventClasses;
    private final IntegrationBroker broker;

    DomesticEventPublisher(IntegrationBroker broker, EventClass messageClass) {
        this.broker = checkNotNull(broker);
        this.eventClasses = ImmutableSet.of(messageClass);
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Returning an immutable impl.
    @Override
    public Set<EventClass> messageClasses() {
        return eventClasses;
    }

    @Override
    public void dispatch(EventEnvelope event) {
        broker.publish(event);
    }

    @Override
    public Set<EventClass> domesticEventClasses() {
        return eventClasses();
    }

    @Override
    public Set<EventClass> externalEventClasses() {
        return ImmutableSet.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DomesticEventPublisher)) {
            return false;
        }
        DomesticEventPublisher publisher = (DomesticEventPublisher) o;
        return Objects.equal(eventClasses, publisher.eventClasses);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(eventClasses);
    }
}
