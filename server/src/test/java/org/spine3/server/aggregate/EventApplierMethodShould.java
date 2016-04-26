/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.base.EventContext;
import org.spine3.test.project.Project;
import org.spine3.test.project.event.ProjectCreated;

import static org.junit.Assert.assertTrue;
import static org.spine3.test.Verify.assertContains;

@SuppressWarnings("InstanceMethodNamingConvention")
public class EventApplierMethodShould {

    private static class AggregateWithTwoMethodsApplier extends Aggregate<Long, Project, Project.Builder> {

        public AggregateWithTwoMethodsApplier(Long id) {
            super(id);
        }

        @Apply
        private void apply(ProjectCreated event, EventContext context) {
            // Do nothing.
        }
    }

    @Test
    public void do_not_accept_methods_with_two_parameters() {
        assertTrue(Aggregate.getEventClasses(AggregateWithTwoMethodsApplier.class)
                            .isEmpty());
    }

    private static class AggregateWithNonPrivateApplier extends Aggregate<Long, Project, Project.Builder> {

        public AggregateWithNonPrivateApplier(Long id) {
            super(id);
        }

        @Apply
        public void apply(ProjectCreated event) {
            // Do nothing.
        }
    }

    @Test
    public void accept_non_private_appliers() {
        final ImmutableSet<Class<? extends Message>> eventClasses = Aggregate.getEventClasses(
                AggregateWithNonPrivateApplier.class);

        // The method is counted and the event is present.
        assertContains(ProjectCreated.class, eventClasses);
    }

}
