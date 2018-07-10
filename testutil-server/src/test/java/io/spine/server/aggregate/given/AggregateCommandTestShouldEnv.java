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

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.base.ThrowableMessage;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateCommandTest;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.entity.Repository;
import io.spine.server.entity.given.Given;
import io.spine.server.expected.CommandExpected;
import io.spine.time.LocalDate;
import io.spine.time.MonthOfYear;
import io.spine.validate.StringValueVBuilder;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Vladyslav Lubenskyi
 */
public class AggregateCommandTestShouldEnv {

    private static final long ID = 1L;

    /**
     * Prevents instantiation of this utility class.
     */
    private AggregateCommandTestShouldEnv() {
    }

    public static TimePrinter aggregate() {
        TimePrinter result = Given.aggregateOfClass(TimePrinter.class)
                                  .withId(ID)
                                  .withVersion(64)
                                  .build();
        return result;
    }

    /**
     * A dummy aggregate that accepts a {@code Timestamp} as a command message
     * and prints it into its state.
     */
    public static final class TimePrinter
            extends Aggregate<Long, StringValue, StringValueVBuilder> {

        TimePrinter(Long id) {
            super(id);
        }

        @Assign
        public Timestamp handle(Timestamp timestamp) {
            return timestamp;
        }

        @Assign
        public Timestamp handle(LocalDate command) throws TestRejection {
            throw new TestRejection(command);
        }

        @Apply
        void on(Timestamp timestamp) {
            getBuilder().setValue(Timestamps.toString(timestamp));
        }
    }

    public static final class TestRejection extends ThrowableMessage {

        private static final long serialVersionUID = 0;

        TestRejection(GeneratedMessageV3 message) {
            super(message);
        }
    }

    private static final class TimePrinterRepository extends AggregateRepository<Long, TimePrinter> {
    }

    /**
     * The test class for the {@code TimePrinter} only command handler.
     */
    public static class TimePrintingTest
            extends AggregateCommandTest<Timestamp, Long, StringValue, TimePrinter> {

        public static final Timestamp TEST_COMMAND = Timestamp.newBuilder()
                                                              .setNanos(1024)
                                                              .build();

        @Override
        protected Long newId() {
            return ID;
        }

        @Override
        protected Timestamp createMessage() {
            return TEST_COMMAND;
        }

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        protected Repository<Long, TimePrinter> createEntityRepository() {
            return new TimePrinterRepository();
        }

        @Override
        public CommandExpected<StringValue> expectThat(TimePrinter entity) {
            return super.expectThat(entity);
        }

        public Message storedMessage() {
            return message();
        }
    }

    /**
     * The test class for the {@code TimePrinter} only command handler.
     */
    public static class TimePrintingRejectionTest
            extends AggregateCommandTest<LocalDate, Long, StringValue, TimePrinter> {

        public static final LocalDate TEST_COMMAND = LocalDate.newBuilder()
                                                              .setDay(5)
                                                              .setMonth(MonthOfYear.AUGUST)
                                                              .setYear(2015)
                                                              .build();

        @Override
        protected Long newId() {
            return ID;
        }

        @Override
        protected LocalDate createMessage() {
            return TEST_COMMAND;
        }

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        protected Repository<Long, TimePrinter> createEntityRepository() {
            return new TimePrinterRepository();
        }

        @Override
        public CommandExpected<StringValue> expectThat(TimePrinter entity) {
            return super.expectThat(entity);
        }

        public Message storedMessage() {
            return message();
        }
    }

}
