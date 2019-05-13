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

package io.spine.system.server;

import com.google.protobuf.Empty;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.system.server.event.EntityArchived;
import io.spine.system.server.event.EntityDeleted;
import io.spine.system.server.event.EntityRestored;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.system.server.event.EntityUnarchived;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.system.server.given.mirror.ProjectionTestEnv.ID;
import static io.spine.system.server.given.mirror.ProjectionTestEnv.VERSION;
import static io.spine.system.server.given.mirror.ProjectionTestEnv.entityArchived;
import static io.spine.system.server.given.mirror.ProjectionTestEnv.entityDeleted;
import static io.spine.system.server.given.mirror.ProjectionTestEnv.entityExtracted;
import static io.spine.system.server.given.mirror.ProjectionTestEnv.entityRestored;
import static io.spine.system.server.given.mirror.ProjectionTestEnv.entityStateChanged;
import static io.spine.testing.server.projection.ProjectionEventDispatcher.dispatch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Mirror projection should on")
class MirrorEventsTest {

    private static final String CHANGE_STATE = "change own state";

    private static final TestEventFactory events =
            TestEventFactory.newInstance(MirrorEventsTest.class);

    @Nested
    @DisplayName("EntityStateChanged event")
    class StateChanged extends MirrorProjectionTest<EntityStateChanged> {

        private StateChanged() {
            super(entityStateChanged());
        }

        @Test
        @DisplayName(CHANGE_STATE)
        void testState() {
            expectThat(new MirrorProjection(ID))
                    .hasState(mirror -> {
                        assertEquals(Empty.getDefaultInstance(), unpack(mirror.getState()));
                        assertEquals(ID, mirror.getId());
                        assertFalse(mirror.getLifecycle().getArchived());
                        assertFalse(mirror.getLifecycle().getDeleted());
                        assertTrue(mirror.getColumns().getColumnsMap().isEmpty());
                        assertEquals(VERSION, mirror.getVersion());
                    });
        }
    }

    @Nested
    @DisplayName("EntityArchived event")
    class Archived extends MirrorProjectionTest<EntityArchived> {

        private MirrorProjection projection;

        private Archived() {
            super(entityArchived());
        }

        @BeforeEach
        void prepare() {
            projection = new MirrorProjection(ID);
            play(projection, entityStateChanged());
        }

        @Test
        @DisplayName(CHANGE_STATE)
        void testState() {
            expectThat(projection)
                    .hasState(mirror -> {
                        assertTrue(mirror.getLifecycle().getArchived());
                        assertFalse(mirror.getLifecycle().getDeleted());
                    });
        }
    }

    @Nested
    @DisplayName("EntityDeleted event")
    class Deleted extends MirrorProjectionTest<EntityDeleted> {

        private MirrorProjection projection;

        private Deleted() {
            super(entityDeleted());
        }

        @BeforeEach
        void prepare() {
            projection = new MirrorProjection(ID);
            play(projection, entityStateChanged());
        }

        @Test
        @DisplayName(CHANGE_STATE)
        void testState() {
            expectThat(projection)
                    .hasState(mirror -> {
                        assertFalse(mirror.getLifecycle().getArchived());
                        assertTrue(mirror.getLifecycle().getDeleted());
                    });
        }
    }

    @Nested
    @DisplayName("EntityExtractedFromArchive event")
    class Extracted extends MirrorProjectionTest<EntityUnarchived> {

        private MirrorProjection projection;

        private Extracted() {
            super(entityExtracted());
        }

        @BeforeEach
        void prepare() {
            projection = new MirrorProjection(ID);
            play(projection, entityStateChanged());
            play(projection, entityArchived());
        }

        @Test
        @DisplayName(CHANGE_STATE)
        void testState() {
            expectThat(projection)
                    .hasState(mirror -> {
                        assertFalse(mirror.getLifecycle().getArchived());
                        assertFalse(mirror.getLifecycle().getDeleted());
                    });
        }
    }

    @Nested
    @DisplayName("EntityRestored event")
    class Restored extends MirrorProjectionTest<EntityRestored> {

        private MirrorProjection projection;

        private Restored() {
            super(entityRestored());
        }

        @BeforeEach
        void prepare() {
            projection = new MirrorProjection(ID);
            play(projection, entityStateChanged());
            play(projection, entityDeleted());
        }

        @Test
        @DisplayName(CHANGE_STATE)
        void testState() {
            expectThat(projection)
                    .hasState(mirror -> {
                        assertFalse(mirror.getLifecycle().getArchived());
                        assertFalse(mirror.getLifecycle().getDeleted());
                    });
        }
    }

    private static void play(MirrorProjection projection, EventMessage eventMessage) {
        Event event = events.createEvent(eventMessage);
        dispatch(projection, event);
    }
}
