/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import io.spine.core.BoundedContextName;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.event.EventSubscriber;
import io.spine.server.integration.route.DynamicRouter;

import java.util.Objects;
import java.util.Set;

import static com.google.common.collect.FluentIterable.from;

/**
 * A subscriber to local {@code EventBus}, which publishes each matching domestic event to
 * a remote channel.
 *
 * <p>The events to subscribe are those that are required by external application components
 * at this moment; their set is determined by the {@linkplain RequestForExternalMessages
 * configuration messages}, received by this instance of {@code IntegrationBus}.
 *
 * @author Alex Tymchenko
 * @author Dmitry Ganzha
 */
final class DomesticEventPublisher extends EventSubscriber {

    private final BoundedContextName boundedContextName;
    private final DynamicRouter router;
    private final Set<EventClass> eventClasses;

    DomesticEventPublisher(BoundedContextName boundedContextName,
                           DynamicRouter router,
                           EventClass messageClass) {
        super();
        this.boundedContextName = boundedContextName;
        this.router = router;
        this.eventClasses = ImmutableSet.of(messageClass);
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Returning an immutable impl.
    @Override
    public Set<EventClass> getMessageClasses() {
        return eventClasses;
    }

    @Override
    public Set<String> dispatch(EventEnvelope envelope) {
        final Event event = envelope.getOuterObject();
        final ExternalMessage msg = ExternalMessages.of(event, boundedContextName);
        Iterable<Publisher> channels = router.route(msg);
        final ImmutableSet<String> result =
                from(channels)
                        .transform(
                                new Function<Publisher, String>() {
                                    @Override
                                    public String apply(Publisher input) {
                                        return input.toString();
                                    }
                                })
                        .toSet();
        return result;
    }

    @SuppressWarnings("DuplicateStringLiteralInspection")
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("boundedContextName", boundedContextName)
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
        return Objects.equals(boundedContextName, that.boundedContextName) &&
                Objects.equals(eventClasses, that.eventClasses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundedContextName, eventClasses);
    }
}
