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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import io.spine.core.BoundedContextName;
import io.spine.core.Event;
import io.spine.logging.Logging;
import io.spine.protobuf.AnyPacker;
import io.spine.server.event.EventDispatcher;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.PublisherHub;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.integration.IntegrationChannels.toId;

/**
 * A subscriber to local {@code EventBus}, which publishes each matching domestic event to
 * a remote channel.
 *
 * <p>The events to subscribe are those that are required by external application components
 * at this moment; their set is determined by the {@linkplain RequestForExternalMessages
 * configuration messages}, received by this instance of {@code IntegrationBus}.
 */
final class DomesticEventPublisher implements EventDispatcher<String>, Logging {

    private final BoundedContextName originContextName;
    private final PublisherHub publisherHub;
    private final Set<EventClass> eventClasses;

    DomesticEventPublisher(BoundedContextName originContextName,
                           PublisherHub publisherHub,
                           EventClass messageClass) {
        super();
        this.originContextName = checkNotNull(originContextName);
        this.publisherHub = checkNotNull(publisherHub);
        this.eventClasses = ImmutableSet.of(messageClass);
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Returning an immutable impl.
    @Override
    public Set<EventClass> getMessageClasses() {
        return eventClasses;
    }

    @Override
    public Set<String> dispatch(EventEnvelope envelope) {
        Event event = envelope.outerObject();
        ExternalMessage msg = ExternalMessages.of(event, originContextName);
        ExternalMessageClass messageClass = ExternalMessageClass.of(envelope.messageClass());
        ChannelId channelId = toId(messageClass);
        Publisher channel = publisherHub.get(channelId);
        channel.publish(AnyPacker.pack(envelope.getId()), msg);

        return ImmutableSet.of(channel.toString());
    }

    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(envelope);
        _error(exception,
               "Error publishing event (class: `%s`, ID: `%s`) from bounded context `%s`.",
               envelope.messageClass(), envelope.getId(), originContextName);
    }

    @Override
    public Set<EventClass> getExternalEventClasses() {
        return ImmutableSet.of();
    }

    @Override
    public Optional<ExternalMessageDispatcher<String>> createExternalDispatcher() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("originContextName", originContextName)
                          .add("eventClasses", eventClasses)
                          .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DomesticEventPublisher that = (DomesticEventPublisher) o;
        return Objects.equals(originContextName, that.originContextName) &&
                Objects.equals(eventClasses, that.eventClasses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originContextName, eventClasses);
    }
}
