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

package io.spine.server.event;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.logging.Logging;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.event.model.EventSubscriberClass;
import io.spine.server.event.model.SubscriberMethod;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.tenant.EventOperation;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.MessageEnvelope;
import io.spine.type.MessageClass;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.event.model.EventSubscriberClass.asEventSubscriberClass;
import static java.lang.String.format;

/**
 * The abstract base for objects that can be subscribed to receive events from {@link EventBus}.
 *
 * <p>Objects may also receive events via {@link EventDispatcher}s that can be
 * registered with {@code EventBus}.
 *
 * @see EventBus#register(MessageDispatcher)
 * @see io.spine.core.Subscribe
 */
public abstract class AbstractEventSubscriber
        implements EventDispatcher<String>, EventSubscriber, Logging {

    /** Model class for this subscriber. */
    private final EventSubscriberClass<?> thisClass = asEventSubscriberClass(getClass());

    /**
     * Dispatches the event to the handling method.
     *
     * @param event
     *         the envelope with the event
     * @return the element set with the result of {@link #toString()} as the identify of the
     *         subscriber or empty set if dispatching failed
     */
    @Override
    public final Set<String> dispatch(EventEnvelope event) {
        EventOperation op = new EventOperation(event.outerObject()) {
            @Override
            public void run() {
                handle(event);
            }
        };
        try {
            op.execute();
        } catch (RuntimeException exception) {
            onError(event, exception);
            return ImmutableSet.of();
        }
        return identity();
    }

    /**
     * Handles an event dispatched to this subscriber instance.
     *
     * <p>By default, passes the event to the corresponding {@linkplain io.spine.core.Subscribe
     * subscriber} method of the entity.
     */
    protected void handle(EventEnvelope event) {
        thisClass.subscriberOf(event)
                 .ifPresent(method -> method.invoke(this, event));
    }

    /**
     * Logs the error into the subscriber {@linkplain #log() log}.
     *
     * @param event
     *         the event which caused the error
     * @param exception
     *         the error
     */
    @Override
    public void onError(EventEnvelope event, RuntimeException exception) {
        checkNotNull(event);
        checkNotNull(exception);
        MessageClass messageClass = event.messageClass();
        String messageId = event.idAsString();
        String errorMessage =
                format("Error handling event subscription (class: %s id: %s) in %s.",
                       messageClass, messageId, thisClass);
        log().error(errorMessage, exception);
    }

    @Override
    public boolean canDispatch(EventEnvelope event) {
        Optional<SubscriberMethod> subscriber = thisClass.subscriberOf(event);
        return subscriber.isPresent();
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // as we return an immutable collection.
    public Set<EventClass> messageClasses() {
        return thisClass.incomingEvents();
    }

    @Override
    public Set<EventClass> externalEventClasses() {
        return thisClass.externalEvents();
    }

    @Override
    public Optional<ExternalMessageDispatcher<String>> createExternalDispatcher() {
        return Optional.of(new ExternalDispatcher());
    }

    /**
     * Dispatches external events to this subscriber.
     */
    private final class ExternalDispatcher implements ExternalMessageDispatcher<String>, Logging {

        @Override
        public Set<ExternalMessageClass> messageClasses() {
            return ExternalMessageClass.fromEventClasses(externalEventClasses());
        }

        @CanIgnoreReturnValue
        @Override
        public Set<String> dispatch(ExternalMessageEnvelope envelope) {
            EventEnvelope eventEnvelope = envelope.toEventEnvelope();
            return AbstractEventSubscriber.this.dispatch(eventEnvelope);
        }

        @Override
        public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
            checkNotNull(envelope);
            checkNotNull(exception);
            logError("Error dispatching external event to event subscriber " +
                             "(event class: %s, id: %s)",
                     envelope, exception);
        }

        @Override
        public boolean canDispatch(ExternalMessageEnvelope envelope) {
            EventEnvelope event = envelope.toEventEnvelope();
            return AbstractEventSubscriber.this.canDispatch(event);
        }

        private void logError(String msgFormat,
                              MessageEnvelope envelope,
                              RuntimeException exception) {
            MessageClass messageClass = envelope.messageClass();
            String messageId = envelope.idAsString();
            String errorMessage = format(msgFormat, messageClass, messageId);
            _error(errorMessage, exception);
        }
    }
}
