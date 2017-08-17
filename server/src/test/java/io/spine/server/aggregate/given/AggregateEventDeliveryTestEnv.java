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
package io.spine.server.aggregate.given;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.Identifier;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.React;
import io.spine.protobuf.AnyPacker;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateEventDelivery;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.command.TestEventFactory;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.validate.StringValueVBuilder;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.emptyList;

/**
 * @author Alex Tymchenko
 */
public class AggregateEventDeliveryTestEnv {

    /** Prevents instantiation of this utility class. */
    private AggregateEventDeliveryTestEnv() {
    }

    public static Event projectStarted() {
        final ProjectId projectId = ProjectId.newBuilder()
                                             .setId(Identifier.newUuid())
                                             .build();
        final TestEventFactory eventFactory =
                TestEventFactory.newInstance(
                        AnyPacker.pack(projectId),
                        AggregateEventDeliveryTestEnv.class
                );

        final AggProjectStarted msg = AggProjectStarted.newBuilder()
                                                       .setProjectId(projectId)
                                                       .build();

        final Event result = eventFactory.createEvent(msg);
        return result;
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    public static class ReactingProject
            extends Aggregate<ProjectId, StringValue, StringValueVBuilder> {

        private static AggProjectStarted eventReceived = null;

        protected ReactingProject(ProjectId id) {
            super(id);
        }

        @React
        List<Message> on(AggProjectStarted event) {
            eventReceived = event;
            return emptyList();
        }

        @Nullable
        public static AggProjectStarted getEventReceived() {
            return eventReceived;
        }
    }

    @SuppressWarnings(
            {"AssignmentToStaticFieldFromInstanceMethod",
                    "NonThreadSafeLazyInitialization"})
    public static class PostponedReactingRepository
            extends AggregateRepository<ProjectId, ReactingProject> {

        private static PostponingDelivery delivery = null;

        @Override
        protected PostponingDelivery getEventEndpointDelivery() {
            if (delivery == null) {
                delivery = new PostponingDelivery(this);
            }
            return delivery;
        }

        public static PostponingDelivery getDelivery() {
            return delivery;
        }
    }

    public static class PostponingDelivery extends AggregateEventDelivery<ProjectId, ReactingProject> {

        private final Map<ProjectId, EventEnvelope> postponedEvents = newHashMap();

        protected PostponingDelivery(AggregateRepository<ProjectId, ReactingProject> repository) {
            super(repository);
        }

        @Override
        public boolean shouldPostpone(ProjectId id, EventEnvelope envelope) {
            postponedEvents.put(id, envelope);
            return true;
        }

        public Map<ProjectId, EventEnvelope> getPostponedEvents() {
            return ImmutableMap.copyOf(postponedEvents);
        }
    }
}
