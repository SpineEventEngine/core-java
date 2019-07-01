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

import io.spine.base.EventMessage;
import io.spine.server.entity.LifecycleFlags;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.entity.EntitySubject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.system.server.given.mirror.ProjectionTestEnv.ID;
import static io.spine.system.server.given.mirror.ProjectionTestEnv.entityArchived;
import static io.spine.system.server.given.mirror.ProjectionTestEnv.entityDeleted;
import static io.spine.system.server.given.mirror.ProjectionTestEnv.entityExtracted;
import static io.spine.system.server.given.mirror.ProjectionTestEnv.entityRestored;
import static io.spine.system.server.given.mirror.ProjectionTestEnv.entityStateChanged;

@DisplayName("Mirror projection should")
class MirrorProjectionTest {

    @Test
    @DisplayName("change its state on EntityStateChanged")
    void stateChanged() {
        EntityStateChanged event = entityStateChanged();
        context()
                .receivesEvent(event)
                .assertEntityWithState(Mirror.class, ID)
                .hasStateThat()
                .comparingExpectedFieldsOnly()
                .isEqualTo(Mirror.newBuilder()
                                 .setId(ID)
                                 .setState(event.getNewState())
                                 .setLifecycle(LifecycleFlags.getDefaultInstance())
                                 .buildPartial());
    }

    @Test
    @DisplayName("mark itself archived")
    void archived() {
        EventMessage event = entityArchived();
        EntitySubject assertMirror = context()
                .receivesEvents(entityStateChanged(), event)
                .assertEntityWithState(Mirror.class, ID);
        assertMirror.archivedFlag()
                    .isTrue();
        assertMirror.deletedFlag()
                    .isFalse();
        assertMirror.hasStateThat()
                    .isNotEqualToDefaultInstance();
    }

    @Test
    @DisplayName("mark itself deleted")
    void deleted() {
        EventMessage event = entityDeleted();
        EntitySubject assertMirror = context()
                .receivesEvents(entityStateChanged(), event)
                .assertEntityWithState(Mirror.class, ID);
        assertMirror.archivedFlag()
                    .isFalse();
        assertMirror.deletedFlag()
                    .isTrue();
        assertMirror.hasStateThat()
                    .isNotEqualToDefaultInstance();
    }

    @Test
    @DisplayName("un-archive self")
    void extracted() {
        EventMessage event = entityExtracted();
        EntitySubject assertMirror = context()
                .receivesEvents(entityStateChanged(), entityArchived(), event)
                .assertEntityWithState(Mirror.class, ID);
        assertMirror.archivedFlag()
                    .isFalse();
        assertMirror.deletedFlag()
                    .isFalse();
        assertMirror.hasStateThat()
                    .isNotEqualToDefaultInstance();
    }

    @Test
    @DisplayName("un-delete self")
    void restored() {
        EventMessage event = entityRestored();
        EntitySubject assertMirror = context()
                .receivesEvents(entityStateChanged(), entityDeleted(), event)
                .assertEntityWithState(Mirror.class, ID);
        assertMirror.archivedFlag()
                    .isFalse();
        assertMirror.deletedFlag()
                    .isFalse();
        assertMirror.hasStateThat()
                    .isNotEqualToDefaultInstance();
    }

    private static BlackBoxBoundedContext context() {
        return BlackBoxBoundedContext
                .singleTenant()
                .with(new MirrorRepository());
    }
}
