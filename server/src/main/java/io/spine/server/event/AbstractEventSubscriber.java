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
import com.google.protobuf.Empty;
import io.spine.base.Identifier;
import io.spine.core.BoundedContextName;
import io.spine.core.MessageId;
import io.spine.logging.Logging;
import io.spine.server.BoundedContext;
import io.spine.server.ContextAware;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.entity.Ignore;
import io.spine.server.entity.PropagationOutcome;
import io.spine.server.event.model.EventSubscriberClass;
import io.spine.server.event.model.SubscriberMethod;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.HandlerFailedUnexpectedly;
import io.spine.system.server.NoOpSystemWriteSide;
import io.spine.system.server.SystemWriteSide;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.event.model.EventSubscriberClass.asEventSubscriberClass;
import static io.spine.server.tenant.TenantAwareRunner.with;
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
        implements EventDispatcher<String>, EventSubscriber, ContextAware {

    /** Model class for this subscriber. */
    private final EventSubscriberClass<?> thisClass = asEventSubscriberClass(getClass());
    private final MessageId eventAnchor = MessageId
            .newBuilder()
            .setId(Identifier.pack(getClass().getName()))
            .setTypeUrl(TypeUrl.of(Empty.class).value())
            .vBuild();
    @LazyInit
    private SystemWriteSide system = NoOpSystemWriteSide.INSTANCE;
    @LazyInit
    private @MonotonicNonNull BoundedContextName contextName;

    @Override
    public void registerWith(BoundedContext context) {
        checkNotNull(context);
        checkNotRegistered();
        contextName = context.name();
        system = context.systemClient()
                        .writeSide();

    }

    @Override
    public boolean isRegistered() {
        return contextName != null;
    }

    /**
     * Dispatches the event to the handling method.
     *
     * @param event
     *         the envelope with the event
     */
    @Override
    public final void dispatch(EventEnvelope event) {
        with(event.tenantId()).run(() -> handle(event));
    }

    /**
     * Handles an event dispatched to this subscriber instance.
     *
     * <p>By default, passes the event to the corresponding {@linkplain io.spine.core.Subscribe
     * subscriber} method of the entity.
     */
    protected void handle(EventEnvelope event) {
        PropagationOutcome outcome =
                thisClass.subscriberOf(event)
                         .map(method -> method.invoke(this, event))
                         .orElseGet(() -> notSupported(event));
        if (outcome.hasError()) {
            HandlerFailedUnexpectedly systemEvent = HandlerFailedUnexpectedly
                    .newBuilder()
                    .setEntity(eventAnchor)
                    .setHandledSignal(event.messageId())
                    .setError(outcome.getError())
                    .vBuild();
            system.postEvent(systemEvent, event.asMessageOrigin());
        }
    }

    private PropagationOutcome notSupported(EventEnvelope event) {
        Ignore ignore = Ignore
                .newBuilder()
                .setReason(format("Event %s[%s] does not match subscriber filters in %s.",
                                  event.messageClass(),
                                  event.id().value(),
                                  this.getClass().getCanonicalName()))
                .buildPartial();
        return PropagationOutcome
                .newBuilder()
                .setPropagatedSignal(event.messageId())
                .setIgnored(ignore)
                .vBuild();
    }

    @Override
    public boolean canDispatch(EventEnvelope event) {
        Optional<SubscriberMethod> subscriber = thisClass.subscriberOf(event);
        return subscriber.isPresent();
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // as we return an immutable collection.
    public Set<EventClass> messageClasses() {
        return thisClass.domesticEvents();
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
        public void dispatch(ExternalMessageEnvelope envelope) {
            EventEnvelope eventEnvelope = envelope.toEventEnvelope();
            AbstractEventSubscriber.this.dispatch(eventEnvelope);
        }

        @Override
        public boolean canDispatch(ExternalMessageEnvelope envelope) {
            EventEnvelope event = envelope.toEventEnvelope();
            return AbstractEventSubscriber.this.canDispatch(event);
        }
    }
}
