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
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.mockito.Mockito;
import org.spine3.annotation.Subscribe;
import org.spine3.base.Command;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.server.command.Assign;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.test.TestEventFactory;
import org.spine3.validate.StringValueValidatingBuilder;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Alex Tymchenko
 */
public class ProcessManagerDispatcherShould {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private final TestEventFactory eventFactory = TestEventFactory.newInstance(getClass());

    @Test
    public void pass_the_null_tolerance_check() {
        final Event sampleEvent = eventFactory.createEvent(StringValue.getDefaultInstance());

        final StringValue commandMessage = StringValue.getDefaultInstance();
        final Command sampleCommand = requestFactory.createCommand(commandMessage);

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

    @Test
    public void dispatch_command() {
        final ProcessManager spied = spiedProcMan();

        final Command sampleCommand = requestFactory.createCommand(
                StringValue.getDefaultInstance());
        final CommandEnvelope expectedEnvelope = CommandEnvelope.of(sampleCommand);
        ProcessManagerDispatcher.dispatch(spied, expectedEnvelope);

        verify(spied).dispatchCommand(eq(expectedEnvelope));
    }

    @Test
    public void dispatch_event_passed_as_Event() {
        final ProcessManager spied = spiedProcMan();

        final Int32Value eventMsg = Int32Value.getDefaultInstance();
        final Event event = eventFactory.createEvent(eventMsg);

        ProcessManagerDispatcher.dispatch(spied, event);

        verify(spied).dispatchEvent(eq(eventMsg), eq(event.getContext()));
    }

    @Test
    public void dispatch_event_passed_as_Message_and_EventContext() {
        final ProcessManager spied = spiedProcMan();

        final Int32Value eventMsg = Int32Value.getDefaultInstance();
        final Event event = eventFactory.createEvent(eventMsg);
        final EventContext expectedContext = event.getContext();

        ProcessManagerDispatcher.dispatch(spied, eventMsg, expectedContext);

        verify(spied).dispatchEvent(eq(eventMsg), eq(expectedContext));
    }

    private static ProcessManager spiedProcMan() {
        final SampleProcessManager processManager = new SampleProcessManager(1L);
        return Mockito.spy(processManager);
    }

    @SuppressWarnings("unused")     // Methods accessed via reflection.
    private static class SampleProcessManager
            extends ProcessManager<Long, StringValue, StringValueValidatingBuilder> {

        private static final Int32Value EVENT_MSG = Int32Value.getDefaultInstance();

        private SampleProcessManager(Long id) {
            super(id);
        }

        @Assign
        public Int32Value command(StringValue command) {
            return EVENT_MSG;
        }

        @Subscribe
        public void event(Int32Value event) {
            // do nothing.
        }
    }
}
