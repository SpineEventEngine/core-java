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

import com.google.protobuf.Message;
import io.spine.server.entity.Repository;
import io.spine.testing.server.TUAssignTask;
import io.spine.testing.server.TUProjectId;
import io.spine.testing.server.TUTaskCreated;
import io.spine.testing.server.TUTaskCreationPm;
import io.spine.testing.server.expected.CommanderExpected;
import io.spine.testing.server.procman.PmCommandOnEventTest;
import io.spine.testing.server.procman.given.pm.CommandingPm;
import io.spine.testing.server.procman.given.pm.CommandingPmRepo;
import org.junit.jupiter.api.BeforeEach;

/**
 * A sample test class which we use for testing {@link PmCommandOnEventTest}.
 *
 * <p>It creates test environment for checking that a {@link CommandingPm ProcessManager}
 * creates {@linkplain TUAssignTask a command} in response to an
 * incoming {@linkplain TUTaskCreated event}.
 *
 * @see io.spine.server.procman.given
 */
public class SamplePmCommandOnEventTest
        extends PmCommandOnEventTest<TUProjectId, TUTaskCreated, TUTaskCreationPm, CommandingPm> {

    public static final TUTaskCreated TEST_EVENT =
            TUTaskCreated.newBuilder()
                         .setId(CommandingPm.ID)
                         .build();

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
    }

    @Override
    protected TUProjectId newId() {
        return CommandingPm.ID;
    }

    @Override
    protected TUTaskCreated createMessage() {
        return TEST_EVENT;
    }

    @Override
    protected Repository<TUProjectId, CommandingPm> createEntityRepository() {
        return new CommandingPmRepo();
    }

    /**
     * Exposes {@link #message() to the test.
     * @apiNote we cannot override, since {@codd message()} is {@code final}.
     */
    public Message storedMessage() {
        return message();
    }

    /**
     * Exposes internal configuration method.
     */
    public void init() {
        configureBoundedContext();
    }

    @Override
    public CommanderExpected<TUTaskCreationPm>
    expectThat(CommandingPm entity) {
        return super.expectThat(entity);
    }
}
