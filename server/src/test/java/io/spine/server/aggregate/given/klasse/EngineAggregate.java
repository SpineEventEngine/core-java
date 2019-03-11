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

package io.spine.server.aggregate.given.klasse;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.aggregate.given.klasse.command.StartEngine;
import io.spine.server.aggregate.given.klasse.command.StopEngine;
import io.spine.server.aggregate.given.klasse.event.EmissionTestStarted;
import io.spine.server.aggregate.given.klasse.event.EmissionTestStopped;
import io.spine.server.aggregate.given.klasse.event.EngineStarted;
import io.spine.server.aggregate.given.klasse.event.EngineStopped;
import io.spine.server.aggregate.given.klasse.event.SettingsAdjusted;
import io.spine.server.aggregate.given.klasse.event.TankEmpty;
import io.spine.server.aggregate.given.klasse.rejection.EngineAlreadyStarted;
import io.spine.server.aggregate.given.klasse.rejection.EngineAlreadyStopped;
import io.spine.server.aggregate.given.klasse.rejection.Rejections;
import io.spine.server.command.Assign;
import io.spine.server.event.React;
import io.spine.server.model.Nothing;

import static io.spine.server.aggregate.given.klasse.Engine.Status.STARTED;
import static io.spine.server.aggregate.given.klasse.Engine.Status.STOPPED;

/**
 * A engine which handles commands and reacts on domestic and external events.
 */
public class EngineAggregate extends Aggregate<EngineId, Engine, EngineVBuilder> {

    @Assign
    EngineStarted handle(StartEngine command) throws EngineAlreadyStarted {
        EngineId id = command.getId();
        if (state().getStatus() == STARTED) {
            throw EngineAlreadyStarted
                    .newBuilder()
                    .setId(id)
                    .build();
        }
        return start(id);
    }

    @Apply
    private void on(EngineStarted event) {
        setStarted();
    }

    @Assign
    EngineStopped handle(StopEngine command) throws EngineAlreadyStopped {
        EngineId id = command.getId();
        if (state().getStatus() == STOPPED) {
            throw EngineAlreadyStopped
                    .newBuilder()
                    .setId(id)
                    .build();
        }
        return stop(id);
    }

    @Apply(allowImport = true)
    private void on(EngineStopped event) {
        setStopped();
    }

    /**
     * This is an example of import-only method.
     */
    @Apply(allowImport = true)
    private void on(SettingsAdjusted event) {
        // Do nothing for now.
    }

    /*
     * Domestic event reactions
     ****************************/

    @React
    EngineStopped on(TankEmpty event) {
        return stop(event.getId());
    }

    /*
     * External event reactions
     ****************************/

    @React(external = true)
    Nothing on(EmissionTestStarted event) {
        return nothing();
    }

    @React(external = true)
    Nothing on(EmissionTestStopped event) {
        return nothing();
    }

    /*
     * Domestic rejection reactions
     *
     * Since this class reacts on own rejections (which are derived from
     * ThrowableMessage and have the same names as corresponding rejection
     * message classes), we cannot import the outer class in which they
     * are declared.
     *********************************************************************/

    @React
    Nothing on(Rejections.EngineAlreadyStarted rejection) {
        return nothing();
    }

    @React
    Nothing on(Rejections.EngineAlreadyStopped rejection) {
        return nothing();
    }

    /*
     * External rejection reactions
     *************************************/

    @React(external = true)
    Nothing on(Rejections.CannotStartEmissionTest rejection) {
        return nothing();
    }

    /*
     * Utility methods for creating messages and updating state
     ************************************************************/

    private static EngineStarted start(EngineId id) {
        return EngineStarted.newBuilder()
                            .setId(id)
                            .build();
    }

    private static EngineStopped stop(EngineId id) {
        return EngineStopped.newBuilder()
                            .setId(id)
                            .build();
    }

    private void setStarted() {
        builder().setStatus(STARTED);
    }

    private void setStopped() {
        builder().setStatus(STOPPED);
    }
}
