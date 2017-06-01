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

package org.spine3.server.projection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import org.spine3.annotation.Subscribe;
import org.spine3.server.entity.TestEntityWithStringColumn;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.ProjectValidatingBuilder;
import org.spine3.test.projection.event.ProjectCreated;
import org.spine3.test.projection.event.ProjectStarted;
import org.spine3.test.projection.event.TaskAdded;

/** The projection stub used in tests. */
public class TestProjection
        extends Projection<ProjectId, Project, ProjectValidatingBuilder>
        implements TestEntityWithStringColumn {

    /** The event message history we store for inspecting in delivery tests. */
    private static final Multimap<ProjectId, Message> eventMessagesDelivered =
            HashMultimap.create();

    public TestProjection(ProjectId id) {
        super(id);
    }

    private void keep(Message eventMessage) {
        eventMessagesDelivered.put(getState().getId(), eventMessage);
    }

    static boolean processed(Message eventMessage) {
        final boolean result = eventMessagesDelivered.containsValue(eventMessage);
        return result;
    }

    static void clearMessageDeliveryHistory() {
        eventMessagesDelivered.clear();
    }

    @Subscribe
    public void on(ProjectCreated event) {
        // Keep the event message for further inspection in tests.
        keep(event);

        getBuilder().setId(event.getProjectId())
                    .setStatus(Project.Status.CREATED);
    }

    @Subscribe
    public void on(TaskAdded event) {
        keep(event);
        getBuilder().addTask(event.getTask())
                    .build();
    }

    /**
     * Handles the {@link ProjectStarted} event.
     *
     * @param event   the event message
     */
    @Subscribe
    public void on(ProjectStarted event) {
        keep(event);
        getBuilder().setStatus(Project.Status.STARTED);
    }

    @Override
    public String getIdString() {
        return getId().toString();
    }
}
