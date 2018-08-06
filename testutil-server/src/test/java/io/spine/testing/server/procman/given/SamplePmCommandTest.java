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
import io.spine.testing.server.TUCreateTask;
import io.spine.testing.server.TUProjectId;
import io.spine.testing.server.TUTaskCreationPm;
import io.spine.testing.server.procman.PmCommandTest;
import io.spine.testing.server.procman.given.pm.CommandHandlingPm;
import io.spine.testing.server.procman.given.pm.CommandHandlingPmRepository;
import org.junit.jupiter.api.BeforeEach;

/**
 * The test class for the {@code TUCreateTask} command handler in
 * {@code CommandHandlingProcessManager}.
 */
public class SamplePmCommandTest
        extends PmCommandTest<TUProjectId,
                              TUCreateTask,
                              TUTaskCreationPm,
        CommandHandlingPm> {

    public static final TUCreateTask TEST_COMMAND =
            TUCreateTask.newBuilder()
                        .setId(CommandHandlingPm.ID)
                        .build();

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
    }

    @Override
    protected TUProjectId newId() {
        return CommandHandlingPm.ID;
    }

    @Override
    protected TUCreateTask createMessage() {
        return TEST_COMMAND;
    }

    @Override
    protected Repository<TUProjectId, CommandHandlingPm>
    createEntityRepository() {
        return new CommandHandlingPmRepository();
    }

    public Message storedMessage() {
        return message();
    }

    /**
     * Exposes internal configuration method.
     */
    public void init() {
        configureBoundedContext();
    }
}
