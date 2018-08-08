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

package io.spine.testing.server.procman.given;

import io.spine.server.entity.Repository;
import io.spine.testing.server.given.entity.TuPmState;
import io.spine.testing.server.given.entity.TuProjectId;
import io.spine.testing.server.given.entity.command.TuAssignTask;
import io.spine.testing.server.procman.PmCommandOnCommandTest;
import io.spine.testing.server.procman.given.pm.CommandingPm;

/**
 * A sample class which we use for testing
 * {@link io.spine.testing.server.procman.PmCommandOnCommandTest PmCommandOnCommandTest}.
 *
 * @author Alexander Yevsyukov
 */
public class SamplePmCommandOnCommandTest
        extends PmCommandOnCommandTest<TuProjectId, TuAssignTask, TuPmState, CommandingPm> {

    @Override
    protected TuProjectId entityId() {
        return CommandingPm.ID;
    }

    @Override
    protected TuAssignTask createMessage() {
        //TODO:2018-08-08:alexander.yevsyukov: Implement
        return null;
    }

    @Override
    protected Repository<TuProjectId, CommandingPm> createEntityRepository() {
    //TODO:2018-08-08:alexander.yevsyukov: Implement
        return null;
    }
}
