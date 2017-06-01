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
package org.spine3.server.procman;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.test.TestEventFactory;
import org.spine3.validate.StringValueValidatingBuilder;

import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Alex Tymchenko
 */
public class ProcessManagerDispatcherShould {

    @Test
    public void pass_the_null_tolerance_check() {
        final TestEventFactory factory = TestEventFactory.newInstance(getClass());
        final Event sampleEvent = factory.createEvent(StringValue.getDefaultInstance());

        final TestActorRequestFactory commandFactory =
                TestActorRequestFactory.newInstance(getClass());
        final Command sampleCommand = commandFactory.createCommand(StringValue.getDefaultInstance());


        final NullPointerTester tester = new NullPointerTester();
        tester.setDefault(EventContext.class, EventContext.getDefaultInstance())
              .setDefault(ProcessManager.class, new SampleProcessManager(1L))
              .setDefault(Event.class, sampleEvent)
              .setDefault(CommandEnvelope.class, CommandEnvelope.of(sampleCommand))
              .testStaticMethods(ProcessManagerDispatcher.class,
                                 NullPointerTester.Visibility.PUBLIC);
    }

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(ProcessManagerDispatcher.class);
    }

    private static final class SampleProcessManager
            extends ProcessManager<Long, StringValue, StringValueValidatingBuilder> {

        private SampleProcessManager(Long id) {
            super(id);
        }
    }
}
