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
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import io.spine.base.Identifier;
import io.spine.core.MessageId;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.logging.Logging;
import io.spine.protobuf.TypeConverter;
import io.spine.server.BoundedContext;
import io.spine.server.ContextAware;
import io.spine.server.entity.DispatchOutcome;
import io.spine.server.entity.ProducedEvents;
import io.spine.server.event.model.EventReactorClass;
import io.spine.server.event.model.EventReactorMethod;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.HandlerFailedUnexpectedly;
import io.spine.system.server.NoOpSystemWriteSide;
import io.spine.system.server.SystemWriteSide;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Suppliers.memoize;

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
public abstract class AbstractEventReactor
        implements EventReactor, EventDispatcher<String>, ContextAware {

    private final EventReactorClass<?> thisClass = EventReactorClass.asReactorClass(getClass());
    private final MessageId eventAnchor = MessageId
            .newBuilder()
            .setId(Identifier.pack(getClass().getName()))
            .setTypeUrl(TypeUrl.of(Empty.class).value())
            .vBuild();
    @LazyInit
    private SystemWriteSide system = NoOpSystemWriteSide.INSTANCE;

    /** The event bus to which the emitted events are posted. */
    @LazyInit
    private @MonotonicNonNull EventBus eventBus;

    private final Supplier<Any> producerId =
            memoize(() -> TypeConverter.toAny(getClass().getName()));

    @Override
    public void registerWith(BoundedContext context) {
        checkNotRegistered();
        eventBus = context.eventBus();
        system = context.systemClient().writeSide();
    }

    @Override
    public boolean isRegistered() {
        return eventBus != null;
    }

    @Override
    public Set<EventClass> messageClasses() {
        return thisClass.domesticEvents();
    }

    @CanIgnoreReturnValue
    @Override
    public void dispatch(EventEnvelope event) {
        TenantAwareRunner.with(event.tenantId())
                         .run(() -> reactAndPost(event));
    }

    private void reactAndPost(EventEnvelope event) {
        EventReactorMethod method = thisClass.reactorOf(event.messageClass(),
                                                        event.originClass());
        DispatchOutcome outcome = method.invoke(this, event);
        if (outcome.hasSuccess()) {
            ProducedEvents events = outcome.getSuccess()
                                           .getProducedEvents();
            eventBus.post(events.getEventList());
        } else if (outcome.hasError()) {
            HandlerFailedUnexpectedly systemEvent = HandlerFailedUnexpectedly
                    .newBuilder()
                    .setEntity(eventAnchor)
                    .setHandledSignal(event.messageId())
                    .setError(outcome.getError())
                    .vBuild();
            system.postEvent(systemEvent, event.asMessageOrigin());
        }
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
        public void dispatch(ExternalMessageEnvelope envelope) {
            EventEnvelope eventEnvelope = envelope.toEventEnvelope();
            AbstractEventReactor.this.dispatch(eventEnvelope);
        }
    }
}
