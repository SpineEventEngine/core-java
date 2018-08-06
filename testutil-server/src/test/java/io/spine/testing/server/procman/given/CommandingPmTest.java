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
import io.spine.core.Enrichment;
import io.spine.server.entity.Repository;
import io.spine.testing.server.TUAssignTask;
import io.spine.testing.server.TUProjectId;
import io.spine.testing.server.TUTaskCreated;
import io.spine.testing.server.TUTaskCreationPm;
import io.spine.testing.server.expected.EventHandlerExpected;
import io.spine.testing.server.procman.ProcessManagerEventReactionTest;
import org.junit.jupiter.api.BeforeEach;

import static io.spine.testing.server.procman.given.CommandingPm.NESTED_COMMAND;

/**
 * The test class for the {@code TUTaskCreated} event handler in
 * {@code EventReactingProcessManager}.
 */
public class CommandingPmTest
        extends ProcessManagerEventReactionTest<TUProjectId, TUAssignTask, TUTaskCreationPm, CommandingPm> {

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
    protected TUAssignTask createMessage() {
        return NESTED_COMMAND;
    }

    @Override
    protected Repository<TUProjectId, CommandingPm> createEntityRepository() {
        return new CommandingPmRepo();
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

    @Override
    protected Enrichment enrichment() {
        return Enrichment.newBuilder()
                         .build();
    }

    @Override
    public EventHandlerExpected<TUTaskCreationPm>
    expectThat(CommandingPm entity) {
        return super.expectThat(entity);
    }
}
