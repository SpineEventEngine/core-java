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

import com.google.protobuf.Message;
import io.spine.server.entity.Repository;
import io.spine.testing.server.expected.CommanderExpected;
import io.spine.testing.server.given.entity.TuPmState;
import io.spine.testing.server.given.entity.TuProjectId;
import io.spine.testing.server.given.entity.command.TuAssignTask;
import io.spine.testing.server.given.entity.event.TuProjectCreated;
import io.spine.testing.server.given.entity.event.TuTaskCreated;
import io.spine.testing.server.procman.PmCommandOnEventTest;
import io.spine.testing.server.procman.given.pm.CommandingPm;
import io.spine.testing.server.procman.given.pm.CommandingPmRepo;

/**
 * A sample test class which we use for testing {@link PmCommandOnEventTest}.
 *
 * <p>It creates test environment for checking that a {@link CommandingPm ProcessManager}
 * creates {@linkplain TuAssignTask a command} in response to an
 * incoming {@linkplain TuTaskCreated event}.
 *
 * @see io.spine.testing.server.procman.PmCommandOnEventTestShould
 */
public class SamplePmCommandOnEventTest
        extends PmCommandOnEventTest<TuProjectId, TuProjectCreated, TuPmState, CommandingPm> {

    public static final TuProjectCreated TEST_EVENT =
            TuProjectCreated.newBuilder()
                         .setId(CommandingPm.ID)
                         .build();

    public SamplePmCommandOnEventTest() {
        super(CommandingPm.ID, TEST_EVENT);
    }

    @Override
    protected Repository<TuProjectId, CommandingPm> createRepository() {
        return new CommandingPmRepo();
    }

    /**
     * Exposes {@link #message() to the test.
     *
     * @apiNote we cannot override, since {@codd message()} is {@code final}.
     */
    public Message storedMessage() {
        return message();
    }

    @Override
    public CommanderExpected<TuPmState>
    expectThat(CommandingPm processManager) {
        return super.expectThat(processManager);
    }
}
