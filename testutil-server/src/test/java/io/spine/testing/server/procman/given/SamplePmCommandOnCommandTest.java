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

package io.spine.testing.server.procman.given;

import io.spine.server.entity.Repository;
import io.spine.testing.server.given.entity.TuPmState;
import io.spine.testing.server.given.entity.TuProjectId;
import io.spine.testing.server.given.entity.command.TuCreateProject;
import io.spine.testing.server.procman.PmCommandOnCommandTest;
import io.spine.testing.server.procman.given.pm.CommandingPm;
import io.spine.testing.server.procman.given.pm.CommandingPmRepo;

/**
 * A sample class which we use for testing
 * {@link io.spine.testing.server.procman.PmCommandOnCommandTest PmCommandOnCommandTest}.
 */
@SuppressWarnings("unused")
public class SamplePmCommandOnCommandTest
        extends PmCommandOnCommandTest<TuProjectId, TuCreateProject, TuPmState, CommandingPm> {

    public SamplePmCommandOnCommandTest() {
        super(CommandingPm.ID,
              TuCreateProject.newBuilder()
                             .setId(CommandingPm.ID)
                             .build());
    }

    @Override
    protected Repository<TuProjectId, CommandingPm> createRepository() {
        return new CommandingPmRepo();
    }
}
