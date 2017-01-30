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
import org.spine3.server.aggregate.Aggregate;

import javax.annotation.Nullable;

/**
 * An abstract base for test suites testing aggregate commands.
 *
 * @param <C> the type of the command message that we test in the suite
 * @param <A> the type of the aggregate that handles the command
 * @author Alexander Yevsyukov
 */
public abstract class AggregateCommandTest<C extends Message, A extends Aggregate> extends CommandTest<C> {

    /** The object under the test. */
    @Nullable
    private A aggregate;

    /**
     * {@inheritDoc}
     */
    protected AggregateCommandTest(CommandFactory commandFactory) {
        super(commandFactory);
    }

    /**
     * {@inheritDoc}
     */
    protected AggregateCommandTest() {
        super();
    }

    /**
     * Obtains the aggregate being tested or {@code Optional.absent()} if
     * the test object has not been created yet.
     */
    protected Optional<A> aggregate() {
        return Optional.fromNullable(aggregate);
    }

    /**
     * Creates new test object.
     */
    protected abstract A createAggregate();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() {
        this.aggregate = createAggregate();
    }
}
