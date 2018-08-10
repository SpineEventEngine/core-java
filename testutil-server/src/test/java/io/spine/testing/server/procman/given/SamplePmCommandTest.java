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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.server.entity.Repository;
import io.spine.testing.server.given.entity.TuPmState;
import io.spine.testing.server.given.entity.TuTaskId;
import io.spine.testing.server.given.entity.command.TuCreateTask;
import io.spine.testing.server.procman.PmCommandTest;
import io.spine.testing.server.procman.given.pm.CommandHandlingPm;
import io.spine.testing.server.procman.given.pm.CommandHandlingPmRepo;
import org.junit.jupiter.api.BeforeEach;

/**
 * The test class for the {@code TUCreateTask} command handler in {@link CommandHandlingPm}.
 */
public class SamplePmCommandTest
        extends PmCommandTest<TuTaskId, TuCreateTask, TuPmState, CommandHandlingPm> {

    public static final TuCreateTask TEST_COMMAND =
            TuCreateTask.newBuilder()
                        .setId(CommandHandlingPm.ID)
                        .build();

    public SamplePmCommandTest() {
        super(CommandHandlingPm.ID, TEST_COMMAND);
    }

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
    }

    @Override
    protected Repository<TuTaskId, CommandHandlingPm>
    createEntityRepository() {
        return new CommandHandlingPmRepo();
    }

    /** Exposes protected method for test verification. */
    @VisibleForTesting
    public Message storedMessage() {
        return message();
    }
}
