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

package org.spine3.testdata;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.base.EventId;
import org.spine3.protobuf.Values;
import org.spine3.server.command.EventFactory;
import org.spine3.server.integration.IntegrationEventContext;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.users.TenantId;

import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;

/**
 * Creates event contexts for tests.
 *
 * @author Alexander Litus
 * @deprecated use {@link org.spine3.test.TestEventFactory TestEventFactory}
 */
@SuppressWarnings({"UtilityClass", "OverloadedMethodsWithSameNumberOfParameters"})
@Deprecated
public class TestEventContextFactory {

    public static final Any AGGREGATE_ID = Values.pack(newUuid());
    private static final String TEST_BC_NAME = "Test BC";

    private TestEventContextFactory() {}

    public static EventContext createEventContext(Message aggregateId,
                                                  TenantId tenantId) {
        final EventId eventId = EventFactory.generateId();
        final CommandContext commandContext =
                TestActorRequestFactory.newInstance(TestEventContextFactory.class, tenantId)
                                       .createCommandContext();
        final EventContext.Builder builder = EventContext.newBuilder()
                                                         .setEventId(eventId)
                                                         .setProducerId(pack(aggregateId))
                                                         .setCommandContext(commandContext)
                                                         .setTimestamp(getCurrentTime());
        return builder.build();
    }

    /** Creates a new {@link IntegrationEventContext} with default properties. */
    public static IntegrationEventContext createIntegrationEventContext() {
        final EventId eventId = EventFactory.generateId();
        final IntegrationEventContext.Builder builder =
                IntegrationEventContext.newBuilder()
                                       .setEventId(eventId)
                                       .setTimestamp(getCurrentTime())
                                       .setBoundedContextName(TEST_BC_NAME)
                                       .setProducerId(AGGREGATE_ID);
        return builder.build();
    }

    public static IntegrationEventContext createIntegrationEventContext(Message aggregateId) {
        final EventId eventId = EventFactory.generateId();
        final IntegrationEventContext.Builder builder =
                IntegrationEventContext.newBuilder()
                                       .setEventId(eventId)
                                       .setTimestamp(getCurrentTime())
                                       .setBoundedContextName(TEST_BC_NAME)
                                       .setProducerId(pack(aggregateId));
        return builder.build();
    }
}
