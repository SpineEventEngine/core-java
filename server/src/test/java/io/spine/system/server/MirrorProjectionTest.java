/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.server.BoundedContextBuilder;
import io.spine.server.entity.LifecycleFlags;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.testing.server.blackbox.BlackBoxContext;
import io.spine.testing.server.entity.EntitySubject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.system.server.given.mirror.MirrorProjectionTestEnv.AGGREGATE_TYPE_URL;
import static io.spine.system.server.given.mirror.MirrorProjectionTestEnv.ID;
import static io.spine.system.server.given.mirror.MirrorProjectionTestEnv.entityArchived;
import static io.spine.system.server.given.mirror.MirrorProjectionTestEnv.entityDeleted;
import static io.spine.system.server.given.mirror.MirrorProjectionTestEnv.entityExtracted;
import static io.spine.system.server.given.mirror.MirrorProjectionTestEnv.entityRestored;
import static io.spine.system.server.given.mirror.MirrorProjectionTestEnv.entityStateChanged;

@DisplayName("`MirrorProjection` should")
class MirrorProjectionTest {

    @Test
    @DisplayName("change its state on EntityStateChanged")
    void stateChanged() {
        EntityStateChanged event = entityStateChanged();
        context()
                .receivesEvent(event)
                .assertEntityWithState(ID, Mirror.class)
                .hasStateThat()
                .comparingExpectedFieldsOnly()
                .isEqualTo(Mirror.newBuilder()
                                 .setId(ID)
                                 .setState(event.getNewState())
                                 .buildPartial());
    }

    @Test
    @DisplayName("mark itself archived")
    void archived() {
        EventMessage event = entityArchived();
        EntitySubject assertMirror = context()
                .receivesEvents(entityStateChanged(), event)
                .assertEntityWithState(ID, Mirror.class);
        assertMirror.archivedFlag()
                    .isTrue();
        assertMirror.deletedFlag()
                    .isFalse();
        assertMirror.hasStateThat()
                    .comparingExpectedFieldsOnly()
                    .isEqualTo(Mirror.newBuilder()
                                     .setLifecycle(LifecycleFlags.newBuilder().setArchived(true))
                                     .buildPartial());
    }

    @Test
    @DisplayName("mark itself deleted")
    void deleted() {
        EventMessage event = entityDeleted();
        EntitySubject assertMirror = context()
                .receivesEvents(entityStateChanged(), event)
                .assertEntityWithState(ID, Mirror.class);
        assertMirror.archivedFlag()
                    .isFalse();
        assertMirror.deletedFlag()
                    .isTrue();
        assertMirror.hasStateThat()
                    .isNotEqualToDefaultInstance();
        assertMirror.hasStateThat()
                    .comparingExpectedFieldsOnly()
                    .isEqualTo(Mirror.newBuilder()
                                     .setLifecycle(LifecycleFlags.newBuilder().setDeleted(true))
                                     .buildPartial());
    }

    @Test
    @DisplayName("un-archive self")
    void extracted() {
        EventMessage event = entityExtracted();
        EntitySubject assertMirror = context()
                .receivesEvents(entityStateChanged(), entityArchived(), event)
                .assertEntityWithState(ID, Mirror.class);
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
                .assertEntityWithState(ID, Mirror.class);
        assertMirror.archivedFlag()
                    .isFalse();
        assertMirror.deletedFlag()
                    .isFalse();
        assertMirror.hasStateThat()
                    .isNotEqualToDefaultInstance();
    }

    private static BlackBoxContext context() {
        MirrorRepository mirrorRepository = new MirrorRepository();
        mirrorRepository.addMirroredType(AGGREGATE_TYPE_URL);
        return BlackBoxContext.from(
                BoundedContextBuilder.assumingTests()
                                     .add(mirrorRepository)
        ).tolerateFailures();
    }
}
