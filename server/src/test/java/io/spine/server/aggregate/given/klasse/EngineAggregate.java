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
import io.spine.server.model.NothingHappened;

import static io.spine.server.aggregate.given.klasse.Engine.Status.STARTED;
import static io.spine.server.aggregate.given.klasse.Engine.Status.STOPPED;

/**
 * A engine which handles commands and reacts on domestic and external events.
 *
 * @author Alexander Yevsyukov
 */
public class EngineAggregate extends Aggregate<EngineId, Engine, EngineVBuilder> {

    protected EngineAggregate(EngineId id) {
        super(id);
    }

    @Assign
    EngineStarted handle(StartEngine command) throws EngineAlreadyStarted {
        EngineId id = command.getId();
        if (getState().getStatus() == STARTED) {
            throw new EngineAlreadyStarted(id);
        }
        return start(id);
    }

    @Apply
    void on(EngineStarted event) {
        setStarted();
    }

    @Assign
    EngineStopped handle(StopEngine command) throws EngineAlreadyStopped {
        EngineId id = command.getId();
        if (getState().getStatus() == STOPPED) {
            throw new EngineAlreadyStopped(id);
        }
        return stop(id);
    }

    @Apply(allowImport = true)
    void on(EngineStopped event) {
        setStopped();
    }

    /**
     * This is an example of import-only method.
     */
    @Apply(allowImport = true)
    void on(SettingsAdjusted event) {
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
    NothingHappened on(EmissionTestStarted event) {
        return nothing();
    }

    @React(external = true)
    NothingHappened on(EmissionTestStopped event) {
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
    NothingHappened on(Rejections.EngineAlreadyStarted rejection) {
        return nothing();
    }

    @React
    NothingHappened on(Rejections.EngineAlreadyStopped rejection) {
        return nothing();
    }

    /*
     * External rejection reactions
     *************************************/

    @React(external = true)
    NothingHappened on(Rejections.CannotStartEmissionTest rejection) {
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
        getBuilder().setStatus(STARTED);
    }

    private void setStopped() {
        getBuilder().setStatus(STOPPED);
    }
}
