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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.logging.Logging;
import io.spine.protobuf.TypeConverter;
import io.spine.server.BoundedContext;
import io.spine.server.event.model.EventReactorClass;
import io.spine.server.event.model.EventReactorMethod;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.model.ReactorMethodResult;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.type.MessageClass;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Suppliers.memoize;
import static java.lang.String.format;

/**
 * An abstract base for all classes that may produce events in response to other events.
 *
 * <p>Events may be produced in response to both domestic and external events.
 *
 * <p>To use one, do the following:
 * <ol>
 *     <li>Create an instance, specifying the event bus which receives emitted events.
 *     <li>{@linkplain BoundedContext#registerEventDispatcher(EventDispatcher) Register}.
 * </ol>
 *
 * @see React reactors
 * @see BoundedContext#registerEventDispatcher(EventDispatcher)
 */
public abstract class AbstractEventReactor implements EventReactor, EventDispatcher<String>, Logging {

    private final EventReactorClass<?> thisClass = EventReactorClass.asReactorClass(getClass());

    /** The event bus to which the emitted events are posted. */
    private final EventBus eventBus;

    private final Supplier<Any> producerId =
            memoize(() -> TypeConverter.toAny(getClass().getName()));

    protected AbstractEventReactor(EventBus eventBus) {
        checkNotNull(eventBus);
        this.eventBus = eventBus;
    }

    @Override
    public Set<EventClass> messageClasses() {
        return thisClass.domesticEvents();
    }

    @CanIgnoreReturnValue
    @Override
    public Set<String> dispatch(EventEnvelope event) {
        TenantAwareRunner.with(event.tenantId())
                         .run(() -> reactAndPost(event));
        return identity();
    }

    private void reactAndPost(EventEnvelope event) {
        try {
            EventReactorMethod method = thisClass.reactorOf(event.messageClass(),
                                                            event.originClass());
            ReactorMethodResult result = method.invoke(this, event);
            List<Event> events = result.produceEvents(event);
            eventBus.post(events);
        } catch (RuntimeException ex) {
            onError(event, ex);
        }
    }

    @Override
    public void onError(EventEnvelope event, RuntimeException exception) {
        checkNotNull(event);
        checkNotNull(exception);
        MessageClass messageClass = event.messageClass();
        String messageId = event.idAsString();
        String errorMessage =
                format("Error reacting to event (class: %s id: %s) in %s.",
                       messageClass, messageId, thisClass);
        log().error(errorMessage, exception);
    }

    /**
     * Obtains an external message dispatcher to deliver external messages.
     *
     * <p>Never returns an empty {@code Optional}.
     */
    @Override
    public Optional<ExternalMessageDispatcher<String>> createExternalDispatcher() {
        return Optional.of(new ExternalDispatcher());
    }

    /** Obtains the name of this reactor, {@linkplain TypeConverter#toAny(Object) packed to Any}. */
    @Override
    public Any producerId() {
        return producerId.get();
    }

    /** Returns a {@linkplain Versions#zero() zero version}. */
    @Override
    public Version version() {
        return Versions.zero();
    }

    @Override
    public Set<EventClass> externalEventClasses() {
        return thisClass.externalEvents();
    }

    private final class ExternalDispatcher implements ExternalMessageDispatcher<String>, Logging {

        @Override
        public Set<ExternalMessageClass> messageClasses() {
            return ExternalMessageClass.fromEventClasses(externalEventClasses());
        }

        @CanIgnoreReturnValue
        @Override
        public Set<String> dispatch(ExternalMessageEnvelope envelope) {
            try {
                EventEnvelope eventEnvelope = envelope.toEventEnvelope();
                return AbstractEventReactor.this.dispatch(eventEnvelope);
            } catch (RuntimeException ex) {
                onError(envelope, ex);
            }
            return identity();
        }

        @Override
        public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
            AbstractEventReactor.this.onError(envelope.toEventEnvelope(), exception);
        }
    }
}
