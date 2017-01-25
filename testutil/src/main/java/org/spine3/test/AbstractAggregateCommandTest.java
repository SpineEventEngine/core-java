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

import com.google.protobuf.Message;
import org.spine3.client.CommandFactory;
import org.spine3.server.aggregate.Aggregate;

/**
 * The abstract base for command tests of {@code Aggregate}s and {@code AggregatePart}s.
 *
 * @param <C> the type of the command message that we test in the suite
 * @param <A> the type of the aggregate or the aggregate part that handles the command
 * @author Alexander Yevsyukov
 */
abstract class AbstractAggregateCommandTest<C extends Message, A extends Aggregate> extends CommandTest<C> {

    /**
     * {@inheritDoc}
     */
    protected AbstractAggregateCommandTest(CommandFactory commandFactory) {
        super(commandFactory);
    }

    /**
     * {@inheritDoc}
     */
    protected AbstractAggregateCommandTest() {
        super();
    }

    /**
     * Implement this method to create and store the reference to the object
     * ({@code Aggregate} or {@code AggregatePart}) which is going to be used
     * in the test suite.
     *
     * <p>This method must be called in derived test suites in methods
     * annotated with {@code @Before} (JUnit 4) or {@code @BeforeEach} (JUnit 5).
     */
    protected abstract void setUp();
}
