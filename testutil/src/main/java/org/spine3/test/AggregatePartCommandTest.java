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

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import org.spine3.client.CommandFactory;
import org.spine3.server.aggregate.AggregatePart;

import javax.annotation.Nullable;

/**
 * An abstract base for test suites testing aggregate part commands.
 *
 * @param <C> the type of the command message that we test in the suite
 * @param <P> the type of the aggregate part that handles the command
 * @author Alexander Yevsyukov
 */
public abstract class AggregatePartCommandTest<C extends Message,
                                               P extends AggregatePart> extends CommandTest<C> {
    /** The object under the test. */
    @Nullable
    private P aggregatePart;

    /**
     * {@inheritDoc}
     */
    protected AggregatePartCommandTest(CommandFactory commandFactory) {
        super(commandFactory);
    }

    /**
     * {@inheritDoc}
     */
    protected AggregatePartCommandTest() {
        super();
    }

    /**
     * Creates new test object.
     */
    protected abstract P createAggregatePart();

    /**
     * Obtains the aggregate part being tested or {@code Optional#absent()} if
     * the test object has not been created yet.
     */
    protected Optional<P> aggregatePart() {
        return Optional.fromNullable(aggregatePart);
    }

    /**
     * Initialized a test suite with a newly created {@code AggregatePart}.
     *
     * <p>This method must be called in derived test suites in methods
     * annotated with {@code @Before} (JUnit 4) or {@code @BeforeEach} (JUnit 5).
     */
    @Override
    protected void setUp() {
        this.aggregatePart = createAggregatePart();
    }
}
