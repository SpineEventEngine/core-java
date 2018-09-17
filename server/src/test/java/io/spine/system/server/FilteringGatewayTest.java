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

package io.spine.system.server;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.client.EntityId;
import io.spine.client.Query;
import io.spine.core.Command;
import io.spine.server.entity.EventFilter;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Optional;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static io.spine.base.Identifier.newUuid;
import static io.spine.option.EntityOption.Kind.ENTITY;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.testdata.Sample.messageOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("FilteringGateway should")
class FilteringGatewayTest {

    private static final TestActorRequestFactory requests =
            TestActorRequestFactory.newInstance(FilteringGatewayTest.class);

    @Test
    @DisplayName("not accept nulls on creation")
    void nullTolerance() {
        new NullPointerTester()
                .testStaticMethods(FilteringGateway.class, PACKAGE);
    }

    @Test
    @DisplayName("ignore events discarded by the filter")
    void discardEvents() {
        EventFilter filter = anyEvent -> Optional.empty();
        MemoizingGateway delegate = MemoizingGateway.singleTenant();
        SystemGateway gateway = FilteringGateway.atopOf(delegate, filter);
        gateway.postEvent(newEvent());
        delegate.assertNoEvents();
    }

    @Test
    @DisplayName("alter event message with the EventFilter")
    void alterEventMessage() {
        EventMessage newMessage = newEvent();
        EventFilter filter = systemEvent -> Optional.of(systemEvent.toBuilder()
                                                                   .setMessage(pack(newMessage))
                                                                   .build());
        EventMessage originalMessage = newEvent();
        MemoizingGateway delegate = MemoizingGateway.singleTenant();
        SystemGateway gateway = FilteringGateway.atopOf(delegate, filter);
        gateway.postEvent(originalMessage);
        Message actualEvent = delegate.lastSeenEvent().message();
        assertNotEquals(originalMessage, actualEvent);
        assertEquals(newMessage, actualEvent);
    }

    @Test
    @DisplayName("post given commands 'as-is'")
    void postCommands() {
        EntityHistoryId historyId = historyId();
        Command domainCommand = requests.createCommand(messageOfType(CreateShoppingList.class));
        DispatchCommandToHandler command = DispatchCommandToHandler
                .newBuilder()
                .setReceiver(historyId)
                .setCommand(domainCommand)
                .build();
        MemoizingGateway delegate = MemoizingGateway.singleTenant();
        SystemGateway gateway = FilteringGateway.atopOf(delegate, EventFilter.allowAll());
        gateway.postCommand(command);
        assertEquals(command, delegate.lastSeenCommand().message());
    }

    @Test
    @DisplayName("pass aggregate queries 'as-is'")
    void passQueries() {
        Query query = requests.query().all(Timestamp.class);
        MemoizingGateway delegate = MemoizingGateway.singleTenant();
        SystemGateway gateway = FilteringGateway.atopOf(delegate, EventFilter.allowAll());
        Iterator<Any> iterator = gateway.readDomainAggregate(query);
        assertNotNull(iterator);
        assertEquals(query, delegate.lastSeenQuery().message());
    }

    private static EventMessage newEvent() {
        EntityCreated eventMessage = EntityCreated
                .newBuilder()
                .setId(historyId())
                .setKind(ENTITY)
                .build();
        return eventMessage;
    }

    private static EntityHistoryId historyId() {
        EntityId entityId = EntityId
                .newBuilder()
                .setId(Identifier.pack(newUuid()))
                .build();
        EntityHistoryId historyId = EntityHistoryId
                .newBuilder()
                .setEntityId(entityId)
                .setTypeUrl(TypeUrl.of(Empty.class).value())
                .build();
        return historyId;
    }
}
