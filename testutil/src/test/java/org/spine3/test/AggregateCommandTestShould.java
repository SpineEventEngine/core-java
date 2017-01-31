/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.test;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.junit.Before;
import org.junit.Test;
import org.spine3.client.CommandFactory;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.CommandTestShould.newCommandFactory;

/**
 * @author Alexander Yevsyukov
 */
public class AggregateCommandTestShould {

    private AggregateCommandTest<Timestamp, TimePrinter> aggregateCommandTest;

    @Before
    public void setUp() {
        aggregateCommandTest = new TimePrintingTest();
    }

    @Test
    public void create_an_aggregate_in_setUp() {
        assertFalse(aggregateCommandTest.aggregate().isPresent());

        aggregateCommandTest.setUp();

        assertTrue(aggregateCommandTest.aggregate().isPresent());
    }

    /**
     * Ensures existence of the constructor in {@link AggregateCommandTest} class.
     *
     * <p>We do this by simply invoking the constructor in the derived class.
     * We do not perform checks because they are done in the test suite that checks
     * {@link CommandTest} class.
     */
    @SuppressWarnings("ResultOfObjectAllocationIgnored") // because we don't need the result.
    @Test
    public void has_constructor_with_CommandFactory() {
        new TimePrintingTest(newCommandFactory(getClass()));
    }

    /**
     * A dummy aggregate that accepts a {@code Timestamp} as a command message
     * and prints it into its state.
     */
    private static final class TimePrinter extends Aggregate<Long, StringValue, StringValue.Builder> {

        TimePrinter(Long id) {
            super(id);
        }

        @Assign
        public Timestamp handle(Timestamp timestamp) {
            return timestamp;
        }

        @Apply
        private void on(Timestamp timestamp) {
            getBuilder().setValue(Timestamps.toString(timestamp));
        }
    }

    /**
     * The test harness class that tests how {@code TimePrinter} handles its command.
     */
    private static class TimePrintingTest extends AggregateCommandTest<Timestamp, TimePrinter> {

        private TimePrintingTest(CommandFactory commandFactory) {
            super(commandFactory);
        }

        private TimePrintingTest() {
            super();
        }

        @Override
        protected TimePrinter createAggregate() {
            return new TimePrinter(1L);
        }
    }
}
