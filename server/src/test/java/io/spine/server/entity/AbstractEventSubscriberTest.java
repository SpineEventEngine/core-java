/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.entity;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.base.Time;
import io.spine.client.EntityId;
import io.spine.core.UserId;
import io.spine.core.given.GivenEvent;
import io.spine.server.BoundedContext;
import io.spine.server.groups.Group;
import io.spine.server.groups.TestSubscriber;
import io.spine.server.integration.IntegrationBus;
import io.spine.server.organizations.Organization;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.EntityStateChanged;
import io.spine.system.server.SystemBoundedContexts;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.protobuf.AnyPacker.pack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractEventSubscriberTest {

    private TestSubscriber subscriber;
    private BoundedContext groupsContext;
    private BoundedContext organizationsContext;

    @BeforeEach
    void setUp() {
        InMemoryTransportFactory transport = InMemoryTransportFactory.newInstance();
        groupsContext = BoundedContext
                .newBuilder()
                .setName("Groups")
                .setIntegrationBus(IntegrationBus.newBuilder().setTransportFactory(transport))
                .build();
        organizationsContext = BoundedContext
                .newBuilder()
                .setName("Organizations")
                .setIntegrationBus(IntegrationBus.newBuilder().setTransportFactory(transport))
                .build();
        subscriber = new TestSubscriber();
        groupsContext.registerEventDispatcher(subscriber);
    }

    @Test
    @DisplayName("receive domestic entity state updates")
    void receiveEntityStateUpdates() {
        Group state = Group
                .newBuilder()
                .setName("Admins")
                .addParticipants(UserId.getDefaultInstance())
                .addParticipants(UserId.getDefaultInstance())
                .build();
        EntityStateChanged event = EntityStateChanged
                .newBuilder()
                .setId(historyId(Group.class))
                .setWhen(Time.getCurrentTime())
                .setNewState(pack(state))
                .build();
        SystemBoundedContexts.systemOf(groupsContext)
                             .getEventBus()
                             .post(GivenEvent.withMessage(event));
        Optional<Group> receivedState = subscriber.domestic();
        assertTrue(receivedState.isPresent());
        assertEquals(state, receivedState.get());
        assertFalse(subscriber.external().isPresent());
    }

    @Test
    @DisplayName("receive external entity state updates")
    void receiveExternalEntityStateUpdates() {
        Organization state = Organization
                .newBuilder()
                .setName("Developers")
                .setHead(UserId.getDefaultInstance())
                .addMembers(UserId.getDefaultInstance())
                .addMembers(UserId.getDefaultInstance())
                .build();
        EntityStateChanged event = EntityStateChanged
                .newBuilder()
                .setId(historyId(Organization.class))
                .setWhen(Time.getCurrentTime())
                .setNewState(pack(state))
                .build();
        SystemBoundedContexts.systemOf(organizationsContext)
                             .getEventBus()
                             .post(GivenEvent.withMessage(event));
        Optional<Organization> receivedState = subscriber.external();
        assertTrue(receivedState.isPresent());
        assertEquals(state, receivedState.get());
        assertFalse(subscriber.domestic().isPresent());
    }

    private static EntityHistoryId historyId(Class<? extends Message> type) {
        EntityId entityId = EntityId
                .newBuilder()
                .setId(pack(Empty.getDefaultInstance()))
                .build();
        EntityHistoryId historyId = EntityHistoryId
                .newBuilder()
                .setEntityId(entityId)
                .setTypeUrl(TypeUrl.of(type).value())
                .build();
        return historyId;
    }
}
