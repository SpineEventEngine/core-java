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

import io.spine.core.EventClass;
import io.spine.server.aggregate.ImportEvent;
import io.spine.server.bus.DeadMessageHandler;
import io.spine.server.bus.DispatcherRegistry;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.bus.MulticastBus;

public class ImportBus
        extends MulticastBus<ImportEvent, ImportEnvelope, EventClass, ImportDispatcher<?>> {

    protected ImportBus(AbstractBuilder<ImportEnvelope, ImportEvent, ?> builder) {
        super(builder);
    }

    @Override
    protected DeadMessageHandler<ImportEnvelope> getDeadMessageHandler() {
        //TODO:2018-08-20:alexander.yevsyukov: Implement
        return null;
    }

    @Override
    protected EnvelopeValidator<ImportEnvelope> getValidator() {
        return null;
    }

    @Override
    protected DispatcherRegistry<EventClass, ImportDispatcher<?>> createRegistry() {
        //TODO:2018-08-20:alexander.yevsyukov: Implement
        return null;
    }

    @Override
    protected ImportEnvelope toEnvelope(ImportEvent wrapper) {
        return new ImportEnvelope(wrapper);
    }

    @Override
    protected void dispatch(ImportEnvelope envelope) {
        //TODO:2018-08-20:alexander.yevsyukov: Implement
    }

    @Override
    protected void store(Iterable<ImportEvent> messages) {

    }
}
