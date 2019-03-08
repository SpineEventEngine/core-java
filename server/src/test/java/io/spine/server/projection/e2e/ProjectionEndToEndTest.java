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

package io.spine.server.projection.e2e;

import com.google.common.truth.IterableSubject;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.client.EntityId;
import io.spine.core.ActorContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.Events;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;
import io.spine.server.groups.Group;
import io.spine.server.groups.GroupId;
import io.spine.server.groups.GroupName;
import io.spine.server.groups.GroupNameProjection;
import io.spine.server.groups.GroupProjection;
import io.spine.server.organizations.Organization;
import io.spine.server.organizations.OrganizationEstablished;
import io.spine.server.organizations.OrganizationId;
import io.spine.server.organizations.OrganizationProjection;
import io.spine.server.projection.given.EntitySubscriberProjection;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.GivenEventMessage;
import io.spine.server.projection.given.TestProjection;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.EntityStateChanged;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectTaskNames;
import io.spine.test.projection.event.PrjProjectCreated;
import io.spine.test.projection.event.PrjTaskAdded;
import io.spine.testing.core.given.GivenUserId;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Time.currentTime;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.projection.given.ProjectionRepositoryTestEnv.dispatchedMessageId;
import static io.spine.testing.server.blackbox.verify.state.VerifyState.exactlyOne;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Projection should")
class ProjectionEndToEndTest {

    @Test
    @DisplayName("receive entity state updates from other entities")
    void receiveUpdates() {
        PrjProjectCreated created = GivenEventMessage.projectCreated();
        PrjTaskAdded firstTaskAdded = GivenEventMessage.taskAdded();
        PrjTaskAdded secondTaskAdded = GivenEventMessage.taskAdded();
        ProjectId producerId = created.getProjectId();
        BlackBoxBoundedContext
                .singleTenant()
                .with(new EntitySubscriberProjection.Repository(),
                      new TestProjection.Repository())
                .receivesEventsProducedBy(producerId,
                                          created,
                                          firstTaskAdded,
                                          secondTaskAdded)
                .assertThat(exactlyOne(
                        ProjectTaskNames
                                .newBuilder()
                                .setProjectId(producerId)
                                .setProjectName(created.getName())
                                .addTaskName(firstTaskAdded.getTask()
                                                           .getTitle())
                                .addTaskName(secondTaskAdded.getTask()
                                                            .getTitle())
                                .build()
                ));
    }

    @Test
    @DisplayName("receive entity state updates of entities of other context")
    @SuppressWarnings("ResultOfMethodCallIgnored")
        // Black box context is used in a non-fluent fashion.
    void receiveExternal() {
        OrganizationEstablished established = GivenEventMessage.organizationEstablished();
        SingleTenantBlackBoxContext sender = BlackBoxBoundedContext
                .singleTenant()
                .with(new OrganizationProjection.Repository());
        SingleTenantBlackBoxContext receiver = BlackBoxBoundedContext
                .singleTenant()
                .with(new GroupNameProjection.Repository());
        OrganizationId producerId = established.getId();
        sender.receivesEventsProducedBy(producerId, established);
        receiver.assertThat(exactlyOne(
                GroupName.newBuilder()
                         .setId(GroupId.newBuilder()
                                       .setUuid(producerId.getUuid())
                                       .build())
                         .setName(established.getName())
                         .build()
        ));
    }

    @Test
    @DisplayName("receive entity state updates along with system event context")
    void receiveEntityStateUpdatesAndEventContext() throws Exception {
        GroupProjection.Repository repository = new GroupProjection.Repository();
        BoundedContext groups = BoundedContext
                .newBuilder()
                .build();
        groups.register(repository);
        UserId organizationHead = GivenUserId.newUuid();
        EntityHistoryId historyId = EntityHistoryId
                .newBuilder()
                .setTypeUrl(TypeUrl.of(Organization.class).value())
                .setEntityId(EntityId.newBuilder().setId(pack(organizationHead)))
                .build();
        String organizationName = "Contributors";
        Organization newState = Organization
                .newBuilder()
                .setHead(organizationHead)
                .setName(organizationName)
                .addMembers(UserId.getDefaultInstance())
                .build();
        EntityStateChanged stateChanged = EntityStateChanged
                .newBuilder()
                .setId(historyId)
                .setNewState(pack(newState))
                .setWhen(currentTime())
                .addMessageId(dispatchedMessageId())
                .build();
        Timestamp producedAt = Time.currentTime();
        EventContext eventContext = EventContext
                .newBuilder()
                .setTimestamp(producedAt)
                .setExternal(true)
                .setImportContext(ActorContext.getDefaultInstance())
                .build();
        Event event = Event
                .newBuilder()
                .setId(Events.generateId())
                .setMessage(pack(stateChanged))
                .setContext(eventContext)
                .build();
        Set<GroupId> targets = repository.dispatch(EventEnvelope.of(event));
        assertThat(targets).isNotEmpty();

        Iterator<GroupProjection> allGroups = repository.loadAll();
        assertTrue(allGroups.hasNext());
        GroupProjection singleGroup = allGroups.next();
        assertFalse(allGroups.hasNext());

        Group actualGroup = singleGroup.state();
        assertEquals(actualGroup.getName(), organizationName + producedAt);
        IterableSubject assertParticipants = assertThat(actualGroup.getParticipantsList());
        assertParticipants.containsAllIn(newState.getMembersList());
        assertParticipants.contains(organizationHead);

        groups.close();
    }
}
