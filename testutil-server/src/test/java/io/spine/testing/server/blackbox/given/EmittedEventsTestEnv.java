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

package io.spine.testing.server.blackbox.given;

import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.BbProjectId;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.event.BbProjectCreated;
import io.spine.testing.server.blackbox.event.BbTaskAdded;

import java.util.List;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Identifier.newUuid;

/**
 * @author Mykhailo Drachuk
 */
public class EmittedEventsTestEnv {

    /** Prevents instantiation of this utility class. */
    private EmittedEventsTestEnv() {
        // Does nothing.
    }

    public static List<Event> events(int count, Supplier<Message> messageSupplier) {
        List<Event> events = newArrayList();
        for (int i = 0; i < count; i++) {
            events.add(event(messageSupplier.get()));
        }
        return events;
    }

    public static Event event(Message domainEvent) {
        TestEventFactory factory = eventFactory(requestFactory(newTenantId()));
        return factory.createEvent(domainEvent);
    }

    private static TenantId newTenantId() {
        return TenantId.newBuilder()
                       .setValue(newUuid())
                       .build();
    }

    private static TestEventFactory eventFactory(TestActorRequestFactory requestFactory) {
        return TestEventFactory.newInstance(requestFactory);
    }

    private static TestActorRequestFactory requestFactory(TenantId tenantId) {
        return TestActorRequestFactory.newInstance(BlackBoxBoundedContext.class, tenantId);
    }

    private static BbProjectId newProjectId() {
        return BbProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    public static BbProjectCreated projectCreated() {
        return BbProjectCreated.newBuilder()
                               .setProjectId(newProjectId())
                               .build();
    }

    public static BbTaskAdded taskAdded() {
        return BbTaskAdded.newBuilder()
                          .setProjectId(newProjectId())
                          .build();

    }
}
