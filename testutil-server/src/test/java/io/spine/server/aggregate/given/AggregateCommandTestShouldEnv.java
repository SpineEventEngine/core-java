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

package io.spine.server.aggregate.given;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateCommandTest;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.entity.Repository;
import io.spine.server.entity.given.Given;
import io.spine.server.expected.CommandHandlerExpected;
import io.spine.testutil.server.aggregate.TestUtilAssignProject;
import io.spine.testutil.server.aggregate.TestUtilCreateProject;
import io.spine.testutil.server.aggregate.TestUtilFailedToCreateProject;
import io.spine.testutil.server.aggregate.TestUtilProjectAggregate;
import io.spine.testutil.server.aggregate.TestUtilProjectAggregateVBuilder;
import io.spine.testutil.server.aggregate.TestUtilProjectCreated;
import io.spine.testutil.server.aggregate.TestUtilProjectId;
import org.junit.jupiter.api.BeforeEach;

import static com.google.protobuf.util.Timestamps.fromMillis;

/**
 * @author Vladyslav Lubenskyi
 */
public class AggregateCommandTestShouldEnv {

    private static final TestUtilProjectId ID = TestUtilProjectId.newBuilder()
                                                                 .setValue("project id util")
                                                                 .build();

    /**
     * Prevents instantiation of this utility class.
     */
    private AggregateCommandTestShouldEnv() {
    }

    public static CommandHandlingAggregate aggregate() {
        CommandHandlingAggregate result = Given.aggregateOfClass(CommandHandlingAggregate.class)
                                               .withId(ID)
                                               .withVersion(64)
                                               .build();
        return result;
    }

    /**
     * A dummy aggregate that handles two command messages:
     *
     * <ul>
     *     <li>accepts {@code TestUtilCreateProject}.
     *     <li>rejects {@code TestUtilAssignProject}.
     * </ul>
     */
    public static final class CommandHandlingAggregate
            extends Aggregate<TestUtilProjectId,
                              TestUtilProjectAggregate,
                              TestUtilProjectAggregateVBuilder> {

        CommandHandlingAggregate(TestUtilProjectId id) {
            super(id);
        }

        @Assign
        public TestUtilProjectCreated handle(TestUtilCreateProject command) {
            return TestUtilProjectCreated.getDefaultInstance();
        }

        @Assign
        public Timestamp handle(TestUtilAssignProject command)
                throws TestUtilFailedToCreateProject {
            throw new TestUtilFailedToCreateProject(getId());
        }

        @Apply
        void on(TestUtilProjectCreated event) {
            getBuilder().setTimestamp(fromMillis(1234567));
        }
    }

    private static final class CommandHandlingAggregateRepository
            extends AggregateRepository<TestUtilProjectId, CommandHandlingAggregate> {
    }

    /**
     * The test class for the {@code TestUtilCreateProject} command handler in
     * {@code CommandHandlingAggregate}.
     */
    public static class CommandHandlingTest
            extends AggregateCommandTest<TestUtilProjectId,
                                         TestUtilCreateProject,
                                         TestUtilProjectAggregate,
                                         CommandHandlingAggregate> {

        public static final TestUtilCreateProject TEST_COMMAND =
                TestUtilCreateProject.newBuilder()
                                     .setId(ID)
                                     .build();

        @Override
        protected TestUtilProjectId newId() {
            return ID;
        }

        @Override
        protected TestUtilCreateProject createMessage() {
            return TEST_COMMAND;
        }

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        protected Repository<TestUtilProjectId, CommandHandlingAggregate>
        createEntityRepository() {
            return new CommandHandlingAggregateRepository();
        }

        @Override
        public CommandHandlerExpected<TestUtilProjectAggregate>
        expectThat(CommandHandlingAggregate entity) {
            return super.expectThat(entity);
        }

        public Message storedMessage() {
            return message();
        }
    }

    /**
     * The test class for the {@code TestUtilAssignProject} command handler in
     * {@code CommandHandlingAggregate}.
     */
    public static class RejectionCommandHandlerTest
            extends AggregateCommandTest<TestUtilProjectId,
                                         TestUtilAssignProject,
                                         TestUtilProjectAggregate,
                                         CommandHandlingAggregate> {

        public static final TestUtilAssignProject TEST_COMMAND =
                TestUtilAssignProject.newBuilder()
                                     .setId(ID)
                                     .build();

        @Override
        protected TestUtilProjectId newId() {
            return ID;
        }

        @Override
        protected TestUtilAssignProject createMessage() {
            return TEST_COMMAND;
        }

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        protected Repository<TestUtilProjectId, CommandHandlingAggregate>
        createEntityRepository() {
            return new CommandHandlingAggregateRepository();
        }

        @Override
        public CommandHandlerExpected<TestUtilProjectAggregate>
        expectThat(CommandHandlingAggregate entity) {
            return super.expectThat(entity);
        }

        public Message storedMessage() {
            return message();
        }
    }

}
