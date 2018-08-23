/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.MessageEnvelope;
import io.spine.logging.Logging;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.event.model.EventSubscriberClass;
import io.spine.server.event.model.EventSubscriberMethod;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.tenant.EventOperation;
import io.spine.string.Stringifiers;
import io.spine.type.MessageClass;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.event.model.EventSubscriberClass.asEventSubscriberClass;
import static java.lang.String.format;

/**
 * The abstract base for objects that can be subscribed to receive events from {@link EventBus}.
 *
 * <p>Objects may also receive events via {@link EventDispatcher}s that can be
 * registered with {@code EventBus}.
 *
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 * @see EventBus#register(MessageDispatcher)
 */
public abstract class AbstractEventSubscriber
        implements EventDispatcher<String>, EventSubscriber, Logging {

    /** Model class for this subscriber. */
    private final EventSubscriberClass<?> thisClass = asEventSubscriberClass(getClass());

    /**
     * {@inheritDoc}
     *
     * @param envelope the envelope with the message
     * @return a one element set with the result of {@link #toString()}
     * as the identify of the subscriber, or empty set if dispatching failed
     */
    @Override
    public Set<String> dispatch(EventEnvelope envelope) {
        EventOperation op = new EventOperation(envelope.getOuterObject()) {
            @Override
            public void run() {
                handle(envelope);
            }
        };
        try {
            op.execute();
        } catch (RuntimeException exception) {
            onError(envelope, exception);
            return ImmutableSet.of();
        }
        return identity();
    }

    /**
     * Logs the error into the subscriber {@linkplain #log() log}.
     *
     * @param envelope  the message which caused the error
     * @param exception the error
     */
    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);
        MessageClass messageClass = envelope.getMessageClass();
        String messageId = Stringifiers.toString(envelope.getId());
        String errorMessage =
                format("Error handling event subscription (class: %s id: %s).",
                       messageClass, messageId);
        log().error(errorMessage, exception);
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // as we return an immutable collection.
    public Set<EventClass> getMessageClasses() {
        return thisClass.getEventClasses();
    }

    @Override
    public Set<EventClass> getExternalEventClasses() {
        return thisClass.getExternalEventClasses();
    }

    @Override
    public Optional<ExternalMessageDispatcher<String>> createExternalDispatcher() {
        return Optional.of(new ExternalDispatcher());
    }

    private void handle(EventEnvelope envelope) {
        EventSubscriberMethod method = thisClass.getSubscriber(envelope.getMessageClass(),
                                                               envelope.getOriginClass());
        method.invoke(this, envelope);
    }

    /**
     * Dispatches external events to this subscriber.
     */
    private final class ExternalDispatcher implements ExternalMessageDispatcher<String>, Logging {

        @Override
        public Set<ExternalMessageClass> getMessageClasses() {
            return ExternalMessageClass.fromEventClasses(thisClass.getExternalEventClasses());
        }

        @CanIgnoreReturnValue
        @Override
        public Set<String> dispatch(ExternalMessageEnvelope envelope) {
            ExternalMessage externalMessage = envelope.getOuterObject();
            Event event = unpack(externalMessage.getOriginalMessage());
            EventEnvelope eventEnvelope = EventEnvelope.of(event);
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

        private void logError(String msgFormat,
                              MessageEnvelope envelope,
                              RuntimeException exception) {
            MessageClass messageClass = envelope.getMessageClass();
            String messageId = Stringifiers.toString(envelope.getId());
            String errorMessage = format(msgFormat, messageClass, messageId);
            _error(errorMessage, exception);
        }
    }
}
