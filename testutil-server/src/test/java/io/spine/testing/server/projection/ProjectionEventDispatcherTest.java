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

package io.spine.testing.server.projection;

import io.spine.server.type.EventEnvelope;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.given.entity.TuEventLog;
import io.spine.testing.server.given.entity.TuProjectId;
import io.spine.testing.server.given.entity.event.TuProjectAssigned;
import io.spine.testing.server.given.entity.event.TuProjectCreated;
import io.spine.testing.server.projection.given.prj.TuEventLoggingView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.protobuf.AnyPacker.unpack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProjectionEventDispatcher should")
class ProjectionEventDispatcherTest {

    private static final TuProjectId ID = TuProjectId.newBuilder()
                                                     .setValue("projection id")
                                                     .build();

    private TuEventLoggingView projection;

    @BeforeEach
    void setUp() {
        projection = new TuEventLoggingView(ID);
    }

    @Test
    @DisplayName("dispatch event")
    void dispatchEvent() {
        TestEventFactory factory = TestEventFactory.newInstance(getClass());
        TuProjectCreated firstEvent = TuProjectCreated.newBuilder()
                                                      .setId(ID)
                                                      .build();
        TuProjectAssigned secondEvent = TuProjectAssigned.newBuilder()
                                                         .setId(ID)
                                                         .build();

        EventEnvelope firstEnvelope = EventEnvelope.of(factory.createEvent(firstEvent));
        EventEnvelope secondEnvelope = EventEnvelope.of(factory.createEvent(secondEvent));
        ProjectionEventDispatcher.dispatch(projection, firstEnvelope.message(),
                                           firstEnvelope.getEventContext());
        ProjectionEventDispatcher.dispatch(projection, secondEnvelope.message(),
                                           secondEnvelope.getEventContext());

        TuEventLog state = projection.getState();
        assertEquals(2, state.getEventCount());
        assertTrue(unpack(state.getEvent(0)) instanceof TuProjectCreated);
        assertTrue(unpack(state.getEvent(1)) instanceof TuProjectAssigned);
    }
}
