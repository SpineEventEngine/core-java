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

package io.spine.testing.server.procman;

import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.server.procman.ProcessManager;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.server.TestEventFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for testing command generation in response to incoming event.
 *
 * @param <I> ID message of the process manager
 * @param <M> type of the command to test
 * @param <S> the process manager state type
 * @param <P> the {@link ProcessManager} type
 */
public abstract
class PmCommandOnEventTest<I,
                           M extends EventMessage,
                           S extends Message,
                           P extends ProcessManager<I, S, ?>>
        extends PmCommandGenerationTest<I, M, S, P, EventEnvelope> {

    private final TestEventFactory eventFactory = TestEventFactory.newInstance(getClass());

    protected PmCommandOnEventTest(I processManagerId, M eventMessage) {
        super(processManagerId, eventMessage);
    }

    @Override
    protected final EventEnvelope createEnvelope() {
        M message = message();
        checkNotNull(message);
        Event event = eventFactory.createEvent(message);
        return EventEnvelope.of(event);
    }
}
