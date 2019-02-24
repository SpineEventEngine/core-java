/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.projection.given;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.server.entity.TestEntityWithStringColumn;
import io.spine.server.entity.storage.Column;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectVBuilder;
import io.spine.test.projection.event.PrjProjectArchived;
import io.spine.test.projection.event.PrjProjectCreated;
import io.spine.test.projection.event.PrjProjectDeleted;
import io.spine.test.projection.event.PrjProjectStarted;
import io.spine.test.projection.event.PrjTaskAdded;

import java.util.Set;

/** The projection stub used in tests. */
public class TestProjection
        extends Projection<ProjectId, Project, ProjectVBuilder>
        implements TestEntityWithStringColumn {

    /** The event message history we store for inspecting in delivery tests. */
    private static final Multimap<ProjectId, Message> eventMessagesDelivered =
            HashMultimap.create();

    public TestProjection(ProjectId id) {
        super(id);
    }

    public static boolean processed(Message eventMessage) {
        boolean result = eventMessagesDelivered.containsValue(eventMessage);
        return result;
    }

    /**
     * Returns the IDs of projection instances, which processed the given message.
     *
     * <p>Empty set is returned if no instance processed the given message.
     */
    public static Set<ProjectId> whoProcessed(Message eventMessage) {
        ImmutableSet.Builder<ProjectId> builder = ImmutableSet.builder();
        for (ProjectId projectId : eventMessagesDelivered.keySet()) {
            if (eventMessagesDelivered.get(projectId).contains(eventMessage)) {
                builder.add(projectId);
            }
        }
        return builder.build();
    }

    public static void clearMessageDeliveryHistory() {
        eventMessagesDelivered.clear();
    }

    private void keep(Message eventMessage) {
        eventMessagesDelivered.put(id(), eventMessage);
    }

    @Subscribe
    public void on(PrjProjectCreated event) {
        // Keep the event message for further inspection in tests.
        keep(event);

        Project newState = state().toBuilder()
                                  .setId(event.getProjectId())
                                  .setStatus(Project.Status.CREATED)
                                  .setName(event.getName())
                                  .build();
        getBuilder().mergeFrom(newState);
    }

    @Subscribe
    public void on(PrjTaskAdded event) {
        keep(event);
        getBuilder().addTask(event.getTask());
    }

    /**
     * Handles the {@link io.spine.test.projection.event.PrjProjectStarted} event.
     *
     * @param event
     *         the event message
     * @param ignored
     *         this parameter is left to show that a projection subscriber
     *         can have two parameters
     */
    @Subscribe
    public void on(PrjProjectStarted event,
                   @SuppressWarnings("UnusedParameters") EventContext ignored) {
        keep(event);
        Project newState = state().toBuilder()
                                  .setStatus(Project.Status.STARTED)
                                  .build();
        getBuilder().mergeFrom(newState);
    }

    @Subscribe
    public void on(PrjProjectArchived event) {
        keep(event);
        setArchived(true);
    }

    @Subscribe
    public void on(PrjProjectDeleted event) {
        keep(event);
        setDeleted(true);
    }

    @Column
    public String getName() {
        return state().getName();
    }

    @Override
    public String getIdString() {
        return id().toString();
    }

    public static class Repository
            extends ProjectionRepository<ProjectId, TestProjection, Project> {
    }
}
