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
import io.spine.server.aggregate.given.klasse.command.EngineStarted;
import io.spine.server.aggregate.given.klasse.command.EngineStopped;
import io.spine.server.aggregate.given.klasse.command.StartEngine;
import io.spine.server.aggregate.given.klasse.command.StopEngine;
import io.spine.server.aggregate.given.klasse.rejection.EngineAlreadyStarted;
import io.spine.server.aggregate.given.klasse.rejection.EngineAlreadyStopped;
import io.spine.server.command.Assign;

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
        //TODO:2018-08-19:alexander.yevsyukov: Throw EngineAlreadyStarted if so
        return EngineStarted.newBuilder()
                            .setId(command.getId())
                            .build();
    }

    @Apply
    void on(EngineStarted event) {
        //TODO:2018-08-19:alexander.yevsyukov: Do start.
    }

    @Assign
    EngineStopped handle(StopEngine command) throws EngineAlreadyStopped {
        //TODO:2018-08-19:alexander.yevsyukov: Thow EngineAlreadyStopped if so.
        return EngineStopped.newBuilder()
                            .setId(command.getId())
                            .build();
    }

    @Apply
    void on(EngineStopped event) {
        //TODO:2018-08-19:alexander.yevsyukov: Do stop.
    }
}
