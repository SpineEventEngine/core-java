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

package io.spine.server.aggregate.imports;

import com.google.common.collect.Streams;
import io.spine.core.EventClass;
import io.spine.core.TenantId;
import io.spine.server.aggregate.ImportEvent;
import io.spine.server.bus.BusBuilder;
import io.spine.server.bus.DeadMessageHandler;
import io.spine.server.bus.DispatcherRegistry;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.bus.MessageUnhandled;
import io.spine.server.bus.UnicastBus;
import io.spine.server.tenant.TenantIndex;
import io.spine.system.server.SystemGateway;

import java.util.Optional;

import static io.spine.server.bus.BusBuilder.FieldCheck.gatewayNotSet;
import static io.spine.server.bus.BusBuilder.FieldCheck.tenantIndexNotSet;

/**
 * Dispatches import events to aggregates that import these events.
 *
 * @author Alexander Yevsyukov
 */
public class ImportBus
        extends UnicastBus<ImportEvent, ImportEnvelope, EventClass, ImportDispatcher<?>> {

    private final ImportValidator validator = new ImportValidator();
    private final DeadImportEventHandler deadImportEventHandler = new DeadImportEventHandler();
    private final SystemGateway systemGateway;
    private final TenantIndex tenantIndex;

    private ImportBus(Builder builder) {
        super(builder);
        this.systemGateway = builder.systemGateway()
                                    .orElseThrow(gatewayNotSet());
        this.tenantIndex = builder.tenantIndex()
                                  .orElseThrow(tenantIndexNotSet());
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    protected DeadMessageHandler<ImportEnvelope> getDeadMessageHandler() {
        return deadImportEventHandler;
    }

    @Override
    protected EnvelopeValidator<ImportEnvelope> getValidator() {
        return validator;
    }

    @Override
    protected Registry createRegistry() {
        return new Registry();
    }

    @Override
    protected ImportEnvelope toEnvelope(ImportEvent wrapper) {
        return new ImportEnvelope(wrapper);
    }

    @Override
    protected void dispatch(ImportEnvelope envelope) {
        ImportDispatcher<?> dispatcher = getDispatcher(envelope);
        dispatcher.dispatch(envelope);
    }

    @Override
    protected Registry registry() {
        return (Registry) super.registry();
    }

    /**
     * Does nothing because instances of {@link ImportEvent} are transient.
     */
    @Override
    protected void store(Iterable<ImportEvent> events) {
        TenantId tenantId = tenantOf(events);
        tenantIndex.keep(tenantId);
    }

    private static TenantId tenantOf(Iterable<ImportEvent> events) {
        return Streams.stream(events)
                      .map((e) -> e.getContext()
                                   .getTenantId())
                      .findAny()
                      .orElse(TenantId.getDefaultInstance());
    }

    /**
     * Creates {@link UnsupportedImportEventException} in response to an event message
     * of unsupported type.
     */
    private static class DeadImportEventHandler implements DeadMessageHandler<ImportEnvelope> {

        @Override
        public MessageUnhandled handle(ImportEnvelope envelope) {
            return new UnsupportedImportEventException(envelope);
        }
    }

    /**
     * The registry of import dispatchers.
     *
     * @author Alexander Yevsyukov
     */
    private static final class Registry
            extends DispatcherRegistry<EventClass, ImportDispatcher<?>> {

        @SuppressWarnings("RedundantMethodOverride") // Overrides to open access to the method.
        @Override
        protected Optional<? extends ImportDispatcher<?>> getDispatcher(EventClass messageClass) {
            return super.getDispatcher(messageClass);
        }
    }

    /**
     * The builder for {@link ImportBus}.
     */
    public static class Builder extends BusBuilder<ImportEnvelope, ImportEvent, Builder> {

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
