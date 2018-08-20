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

import com.google.protobuf.Empty;
import io.spine.core.React;
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
    Empty on(EmissionTestStarted event) {
        return empty();
    }

    @React(external = true)
    Empty on(EmissionTestStopped event) {
        return empty();
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
    Empty on(Rejections.EngineAlreadyStarted rejection) {
        return empty();
    }

    @React
    Empty on(Rejections.EngineAlreadyStopped rejection) {
        return empty();
    }

    /*
     * External rejection reactions
     *************************************/

    @React(external = true)
    Empty on(Rejections.CannotStartEmissionTest rejection) {
        return empty();
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

    private static Empty empty() {
        return Empty.getDefaultInstance();
    }

    private void setStarted() {
        getBuilder().setStatus(STARTED);
    }

    private void setStopped() {
        getBuilder().setStatus(STOPPED);
    }
}
