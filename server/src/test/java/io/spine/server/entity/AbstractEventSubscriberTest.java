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

package io.spine.server.entity;

import com.google.common.truth.Truth8;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.client.EntityId;
import io.spine.core.EventId;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;
import io.spine.server.event.model.InsufficientVisibilityError;
import io.spine.server.groups.FilteredStateSubscriber;
import io.spine.server.groups.Group;
import io.spine.server.groups.GroupId;
import io.spine.server.groups.HiddenEntitySubscriber;
import io.spine.server.groups.TestSubscriber;
import io.spine.server.groups.WronglyDomesticSubscriber;
import io.spine.server.groups.WronglyExternalSubscriber;
import io.spine.server.organizations.Organization;
import io.spine.server.organizations.OrganizationId;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.server.type.given.GivenEvent;
import io.spine.system.server.DispatchedMessageId;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.EntityStateChanged;
import io.spine.system.server.SystemBoundedContexts;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.protobuf.AnyPacker.pack;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AbstractEventSubscriber should")
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
                .setTransportFactory(transport)
                .build();
        organizationsContext = BoundedContext
                .newBuilder()
                .setName("Organizations")
                .setTransportFactory(transport)
                .build();
        subscriber = new TestSubscriber();
        groupsContext.registerEventDispatcher(subscriber);
    }

    @Test
    @DisplayName("receive domestic entity state updates")
    void receiveEntityStateUpdates() {
        Group state = Group
                .newBuilder()
                .setId(Identifier.generate(GroupId.class))
                .setName("Admins")
                .addParticipants(UserId.getDefaultInstance())
                .addParticipants(UserId.getDefaultInstance())
                .build();
        EntityStateChanged event = EntityStateChanged
                .newBuilder()
                .setId(historyId(Group.class))
                .setWhen(Time.currentTime())
                .setNewState(pack(state))
                .addMessageId(dispatchedMessageId())
                .build();
        SystemBoundedContexts.systemOf(groupsContext)
                             .eventBus()
                             .post(GivenEvent.withMessage(event));
        Optional<Group> receivedState = subscriber.domestic();
        assertTrue(receivedState.isPresent());
        Truth8.assertThat(receivedState)
              .hasValue(state);
        Truth8.assertThat(subscriber.external())
              .isEmpty();
    }

    @Test
    @DisplayName("receive external entity state updates")
    void receiveExternalEntityStateUpdates() {
        Organization state = Organization
                .newBuilder()
                .setId(Identifier.generate(OrganizationId.class))
                .setName("Developers")
                .setHead(UserId.getDefaultInstance())
                .addMembers(UserId.getDefaultInstance())
                .addMembers(UserId.getDefaultInstance())
                .build();
        EntityStateChanged event = EntityStateChanged
                .newBuilder()
                .setId(historyId(Organization.class))
                .setWhen(Time.currentTime())
                .setNewState(pack(state))
                .addMessageId(dispatchedMessageId())
                .build();
        SystemBoundedContexts.systemOf(organizationsContext)
                             .eventBus()
                             .post(GivenEvent.withMessage(event));
        Optional<Organization> receivedState = subscriber.external();
        Truth8.assertThat(receivedState)
              .hasValue(state);
        Truth8.assertThat(subscriber.domestic())
              .isEmpty();
    }

    @Test
    @DisplayName("fail to subscribe to external events without `external = true`")
    void failIfNotExternal() {
        assertThrows(IllegalArgumentException.class, WronglyDomesticSubscriber::new);
    }

    @Test
    @DisplayName("fail to subscribe to domestic events without `external = false`")
    void failIfNotDomestic() {
        assertThrows(IllegalArgumentException.class, WronglyExternalSubscriber::new);
    }

    @Test
    @DisplayName("fail to subscribe to entity states with filters")
    void failToSubscribeToStateWithFilters() {
        assertThrows(IllegalStateException.class, FilteredStateSubscriber::new);
    }

    @Test
    @DisplayName("fail to subscribe to entity states with insufficient visibility level")
    void failOnInsufficientVisibility() {
        assertThrows(InsufficientVisibilityError.class, HiddenEntitySubscriber::new);
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

    private static DispatchedMessageId dispatchedMessageId() {
        return DispatchedMessageId
                .newBuilder()
                .setEventId(EventId.getDefaultInstance())
                .build();
    }
}
