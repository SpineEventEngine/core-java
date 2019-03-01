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
import com.google.protobuf.Any;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.logging.Logging;
import io.spine.server.event.model.EventReactorClass;
import io.spine.server.event.model.EventReactorMethod;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.model.ReactorMethodResult;
import io.spine.server.tenant.EventOperation;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * An abstract base for all event reactors.
 *
 * @see React reactors
 */
public abstract class AbstractEventReactor implements EventReactor, EventDispatcher<String>, Logging {

    private final EventReactorClass<?> thisClass = EventReactorClass.asReactorClass(getClass());
    private final EventBus eventBus;

    protected AbstractEventReactor(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public Set<EventClass> messageClasses() {
        return thisClass.eventClasses();
    }

    @CanIgnoreReturnValue
    @Override
    public Set<String> dispatch(EventEnvelope envelope) {
        EventOperation op = new EventOperation(envelope.outerObject()) {
            @Override
            public void run() {
                handle(envelope);
            }
        };
        op.run();
        try {
            op.execute();
        } catch (RuntimeException exception) {
            onError(envelope, exception);
            return ImmutableSet.of();
        }

        return identity();
    }

    protected void handle(EventEnvelope envelope) {
        List<Event> events = reactTo(envelope);
        this.eventBus.post(events);
    }

    private List<Event> reactTo(EventEnvelope event) {
        EventReactorMethod method =
                thisClass.getReactor(event.messageClass(), event.originClass());
        ReactorMethodResult result =
                method.invoke(this, event);
        return result.produceEvents(event);
    }

    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        _debug("bad");
    }

    @Override
    public Optional<ExternalMessageDispatcher<String>> createExternalDispatcher() {
        return Optional.of(new ExternalDispatcher());
    }

    @Override
    public Any producerId() {
        return Any.getDefaultInstance();
    }

    @Override
    public Version version() {
        return Versions.zero();
    }

    @Override
    public Set<EventClass> externalEventClasses() {
        return thisClass.externalEventClasses();
    }

    private final class ExternalDispatcher implements ExternalMessageDispatcher<String>, Logging {

        @Override
        public Set<ExternalMessageClass> messageClasses() {
            return ExternalMessageClass.fromEventClasses(externalEventClasses());
        }

        @CanIgnoreReturnValue
        @Override
        public Set<String> dispatch(ExternalMessageEnvelope envelope) {
            EventEnvelope eventEnvelope = envelope.toEventEnvelope();
            return AbstractEventReactor.this.dispatch(eventEnvelope);
        }

        @Override
        public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
            _debug("bad");
        }
    }
}
