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
package org.spine3.server.failure;

import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.base.Failure;
import org.spine3.base.Failures;
import org.spine3.change.StringChange;
import org.spine3.server.event.Subscribe;
import org.spine3.test.failure.ProjectFailures;
import org.spine3.test.failure.ProjectId;
import org.spine3.test.failure.command.UpdateProjectName;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.spine3.base.Identifiers.newUuid;

/**
 * @author Alex Tymchenko
 */
public class FailureBusShould {

    private FailureBus failureBus;

    @Before
    public void setUp() {
        this.failureBus = FailureBus.newBuilder()
                                    .build();
    }

    @Test
    public void have_builder() {
        assertNotNull(FailureBus.newBuilder());
    }

    @Test
    public void return_associated_DispatcherDelivery() {
        final DispatcherFailureDelivery delivery = mock(DispatcherFailureDelivery.class);
        final FailureBus result = FailureBus.newBuilder()
                                            .setDispatcherFailureDelivery(delivery)
                                            .build();
        assertEquals(delivery, result.delivery());
    }

    @Test
    public void return_direct_DispatcherDelivery_if_none_customized() {
        final DispatcherFailureDelivery actual = failureBus.delivery();
        final DispatcherFailureDelivery expected = DispatcherFailureDelivery.directDelivery();
        assertEquals(expected, actual);
    }

    @Test   // as the FailureBus instances do not support enrichment yet.
    public void not_enrich_failure_messages() {
        final Failure original = Failure.getDefaultInstance();
        final Failure enriched = failureBus.enrich(original);
        assertEquals(original, enriched);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_object_with_no_subscriber_methods() {
        failureBus.register(new FailureSubscriber());
    }

    @Test
    public void register_failure_subscriber() {
        final FailureSubscriber subscriberOne = new InvalidProjectNameSubscriber();
        final FailureSubscriber subscriberTwo = new InvalidProjectNameSubscriber();

        failureBus.register(subscriberOne);
        failureBus.register(subscriberTwo);

        final FailureClass failureClass = FailureClass.of(ProjectFailures.InvalidProjectName.class);
        assertTrue(failureBus.hasDispatchers(failureClass));

        final Collection<FailureDispatcher> dispatchers = failureBus.getDispatchers(failureClass);
        assertTrue(dispatchers.contains(subscriberOne));
        assertTrue(dispatchers.contains(subscriberTwo));
    }

    @Test
    public void unregister_subscribers() {
        final FailureSubscriber subscriberOne = new InvalidProjectNameSubscriber();
        final FailureSubscriber subscriberTwo = new InvalidProjectNameSubscriber();
        failureBus.register(subscriberOne);
        failureBus.register(subscriberTwo);
        final FailureClass failureClass = FailureClass.of(
                ProjectFailures.InvalidProjectName.class);

        failureBus.unregister(subscriberOne);

        // Check that the 2nd subscriber with the same failure subscriber method remains
        // after the 1st subscriber unregisters.
        final Collection<FailureDispatcher> subscribers = failureBus.getDispatchers(failureClass);
        assertFalse(subscribers.contains(subscriberOne));
        assertTrue(subscribers.contains(subscriberTwo));

        // Check that after 2nd subscriber us unregisters he's no longer in
        failureBus.unregister(subscriberTwo);

        assertFalse(failureBus.getDispatchers(failureClass)
                              .contains(subscriberTwo));
    }

    @Test
    public void call_subscriber_when_failure_posted() {
        final InvalidProjectNameSubscriber subscriber = new InvalidProjectNameSubscriber();

        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId(newUuid())
                                             .build();
        final ProjectFailures.InvalidProjectName invalidProjectName =
                ProjectFailures.InvalidProjectName.newBuilder()
                                                  .setProjectId(projectId)
                                                  .build();
        final StringChange nameChange = StringChange.newBuilder()
                                                    .setNewValue("Too short")
                                                    .build();
        final UpdateProjectName updateProjectName = UpdateProjectName.newBuilder()
                                                                     .setId(projectId)
                                                                     .setNameUpdate(nameChange)
                                                                     .build();
        final Command command = Commands.createCommand(updateProjectName,
                                                       CommandContext.getDefaultInstance());
        final Failure failure = Failures.createFailure(invalidProjectName, command);
        failureBus.register(subscriber);

        failureBus.post(failure);

        assertEquals(failure, subscriber.getFailureHandled());
    }

    private static class InvalidProjectNameSubscriber extends FailureSubscriber {

        private Failure failureHandled;

        @Subscribe
        public void on(ProjectFailures.InvalidProjectName failure,
                       UpdateProjectName commandMessage,
                       CommandContext context) {
            final Command command = Commands.createCommand(commandMessage, context);
            this.failureHandled = Failures.createFailure(failure, command);
        }

        private Failure getFailureHandled() {
            return failureHandled;
        }
    }
}
