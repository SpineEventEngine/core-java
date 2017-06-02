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
package org.spine3.server.aggregate;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.server.command.Assign;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.validate.StringValueValidatingBuilder;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.spine3.envelope.CommandEnvelope.of;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Alex Tymchenko
 */
public class AggregateCommandDispatcherShould {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    @Test
    public void pass_the_null_tolerance_check() {
        final Command sampleCommand = requestFactory.createCommand(StringValue.getDefaultInstance());

        final NullPointerTester tester = new NullPointerTester();
        tester.setDefault(CommandEnvelope.class, of(sampleCommand))
              .setDefault(Aggregate.class, new SampleAggregate(1L))
              .testStaticMethods(AggregateCommandDispatcher.class,
                                 NullPointerTester.Visibility.PUBLIC);
    }

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(AggregateCommandDispatcher.class);
    }

    @Test
    public void dispatchCommandAndApplyEvents() {
        final Int32Value eventMessage = Int32Value.getDefaultInstance();

        final SampleAggregate aggregate = new SampleAggregate(1L);
        final Aggregate spied = spy(aggregate);
        final List<Int32Value> messages = newArrayList(eventMessage);

        final Command sampleCommand = requestFactory.createCommand(StringValue.getDefaultInstance());
        final CommandEnvelope expectedEnvelope = of(sampleCommand);
        AggregateCommandDispatcher.dispatch(spied, expectedEnvelope);

        verify(spied).dispatchCommand(eq(expectedEnvelope));
        verify(spied).apply(eq(messages), eq(expectedEnvelope));
    }

    @SuppressWarnings("unused")     // Methods are accessed via reflection.
    private static class SampleAggregate
            extends Aggregate<Long, StringValue, StringValueValidatingBuilder> {

        private static final Int32Value EVENT_MSG = Int32Value.getDefaultInstance();

        private SampleAggregate(Long id) {
            super(id);
        }

        @Assign
        public Int32Value onCommand(StringValue command) {
            return EVENT_MSG;
        }

        @Apply
        public void event(Int32Value event) {
            // do nothing.
        }
    }
}
