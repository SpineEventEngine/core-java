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

package io.spine.server.aggregate;

import com.google.common.collect.Streams;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.TenantId;
import io.spine.server.bus.BusBuilder;
import io.spine.server.bus.DeadMessageHandler;
import io.spine.server.bus.DispatcherRegistry;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.bus.MessageUnhandled;
import io.spine.server.bus.UnicastBus;
import io.spine.server.event.EventDispatcher;
import io.spine.server.tenant.TenantIndex;

import java.util.Optional;

import static io.spine.server.bus.BusBuilder.FieldCheck.tenantIndexNotSet;

/**
 * Dispatches import events to aggregates that import these events.
 *
 * @author Alexander Yevsyukov
 */
public final class ImportBus
        extends UnicastBus<Event, EventEnvelope, EventClass, EventDispatcher<?>> {

    private final ImportValidator validator = new ImportValidator();
    private final DeadImportEventHandler deadImportEventHandler = new DeadImportEventHandler();
    private final TenantIndex tenantIndex;

    private ImportBus(Builder builder) {
        super(builder);
        this.tenantIndex = builder.tenantIndex()
                                  .orElseThrow(tenantIndexNotSet());
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    protected DeadMessageHandler<EventEnvelope> getDeadMessageHandler() {
        return deadImportEventHandler;
    }

    @Override
    protected EnvelopeValidator<EventEnvelope> getValidator() {
        return validator;
    }

    @Override
    protected Registry createRegistry() {
        return new Registry();
    }

    @Override
    protected EventEnvelope toEnvelope(Event wrapper) {
        return EventEnvelope.of(wrapper);
    }

    @Override
    protected void dispatch(EventEnvelope envelope) {
        EventDispatcher<?> dispatcher = getDispatcher(envelope);
        dispatcher.dispatch(envelope);
    }

    @Override
    protected Registry registry() {
        return (Registry) super.registry();
    }

    /**
     * Updates the {@link io.spine.server.tenant.TenantIndex TenantIndex} with the ID
     * obtained from the passed events.
     */
    @Override
    protected void store(Iterable<Event> events) {
        TenantId tenantId = tenantOf(events);
        tenantIndex.keep(tenantId);
    }

    private static TenantId tenantOf(Iterable<Event> events) {
        return Streams.stream(events)
                      .map((e) -> e.getContext()
                                   .getImportContext()
                                   .getTenantId())
                      .findAny()
                      .orElse(TenantId.getDefaultInstance());
    }

    /**
     * Creates {@link UnsupportedImportEventException} in response to an event message
     * of unsupported type.
     */
    private static class DeadImportEventHandler implements DeadMessageHandler<EventEnvelope> {

        @Override
        public MessageUnhandled handle(EventEnvelope envelope) {
            return new UnsupportedImportEventException(envelope);
        }
    }

    /**
     * The registry of import dispatchers.
     *
     * @author Alexander Yevsyukov
     */
    private static final class Registry
            extends DispatcherRegistry<EventClass, EventDispatcher<?>> {

        @SuppressWarnings("RedundantMethodOverride") // Overrides to open access to the method.
        @Override
        protected
        Optional<? extends EventDispatcher<?>> getDispatcher(EventClass messageClass) {
            return super.getDispatcher(messageClass);
        }
    }

    /**
     * The builder for {@link ImportBus}.
     */
    public static class Builder extends BusBuilder<EventEnvelope, Event, Builder> {

        /** Prevents direct instantiation. */
        private Builder() {
            super();
        }

        @Override
        public ImportBus build() {
            return new ImportBus(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
