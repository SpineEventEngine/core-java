/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.event;

import io.grpc.stub.StreamObserver;
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.core.Ack;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.Events;
import io.spine.core.MessageId;
import io.spine.core.UserId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.event.given.EventRootCommandIdTestEnv.ProjectAggregateRepository;
import io.spine.server.event.given.EventRootCommandIdTestEnv.TeamAggregateRepository;
import io.spine.server.event.given.EventRootCommandIdTestEnv.TeamCreationRepository;
import io.spine.server.event.given.EventRootCommandIdTestEnv.UserSignUpRepository;
import io.spine.server.type.given.GivenEvent;
import io.spine.test.event.EvInvitationAccepted;
import io.spine.test.event.EvTeamMemberAdded;
import io.spine.test.event.EvTeamProjectAdded;
import io.spine.test.event.ProjectCreated;
import io.spine.testing.logging.MuteLogging;
import io.spine.time.ZoneIds;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.event.EventFactory.forImport;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.TENANT_ID;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.acceptInvitation;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.addTasks;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.addTeamMember;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.allEventsQuery;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.command;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.createProject;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.inviteTeamMembers;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.projectId;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.teamId;
import static io.spine.server.tenant.TenantAwareRunner.with;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Event root message should")
public class EventRootMessageIdTest {

    private BoundedContext context;

    @BeforeEach
    void setUp() {
        context = BoundedContextBuilder
                .assumingTests(true)
                .build();
        ProjectAggregateRepository projectRepository = new ProjectAggregateRepository();
        TeamAggregateRepository teamRepository = new TeamAggregateRepository();
        TeamCreationRepository teamCreationRepository = new TeamCreationRepository();
        UserSignUpRepository userSignUpRepository = new UserSignUpRepository();

        BoundedContext.InternalAccess contextAccess = context.internalAccess();
        contextAccess.register(projectRepository);
        contextAccess.register(teamRepository);
        contextAccess.register(teamCreationRepository);
        contextAccess.register(userSignUpRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
    }

    @Test
    @DisplayName("be equal to the event's own ID if event was imported")
    void noRoots() {
        UserId actorId = UserId
                .newBuilder()
                .setValue(EventRootMessageIdTest.class.getSimpleName())
                .build();
        ActorContext actor = ActorContext
                .newBuilder()
                .setTimestamp(Time.currentTime())
                .setActor(actorId)
                .setZoneId(ZoneIds.systemDefault())
                .build();
        EventFactory events = forImport(actor, Identifier.pack("test"));
        Event event = events.createEvent(GivenEvent.message(), null);

        assertThat(event.rootMessage())
                .isEqualTo(event.messageId());
        assertThat(event.context().rootMessage())
                .isEmpty();
    }

    @Test
    @MuteLogging
    @DisplayName("be equal to the event's own ID by default")
    void empty() {
        Event invalidEvent = Event
                .newBuilder()
                .setId(Events.generateId())
                .setMessage(pack(GivenEvent.message()))
                .buildPartial();
        assertThat(invalidEvent.rootMessage())
                .isEqualTo(invalidEvent.messageId());
    }

    @Nested
    @DisplayName("match ID of command handled by")
    class MatchCommandHandledBy {

        @Test
        @DisplayName("aggregate")
        void aggregate() {
            Command command = command(createProject(projectId(), teamId()));

            postCommand(command);

            List<Event> events = readEvents();
            assertIsRootCommand(command, events.get(0));
        }

        @Test
        @DisplayName("aggregate in case command returns multiple events")
        void aggregateForMultipleEvents() {
            Command command = command(addTasks(projectId(), 3));
            postCommand(command);

            List<Event> events = readEvents();
            assertThat(events).hasSize(3);
            assertIsRootCommand(command, events.get(0));
            assertIsRootCommand(command, events.get(1));
            assertIsRootCommand(command, events.get(2));
        }

        @Test
        @DisplayName("process manager")
        void processManager() {
            Command command = command(addTeamMember(teamId()));
            postCommand(command);

            List<Event> events = readEvents();
            assertThat(events).hasSize(1);
            assertIsRootCommand(command, events.get(0));
        }

        @Test
        @DisplayName("process manager in case command returns multiple events")
        void processManagerForMultipleEvents() {
            Command command = command(inviteTeamMembers(teamId(), 3));
            postCommand(command);

            List<Event> events = readEvents();
            assertThat(events).hasSize(3);
            assertIsRootCommand(command, events.get(0));
            assertIsRootCommand(command, events.get(1));
            assertIsRootCommand(command, events.get(2));
        }

        /**
         * Asserts that the ID of the passed command is the root command ID of the passed event.
         */
        private void assertIsRootCommand(Command command, Event event) {
            assertThat(event.rootMessage())
                    .isEqualTo(command.messageId());
        }
    }

    @Nested
    @DisplayName("match ID of external event handled by")
    class MatchExternalEventHandledBy {

        /**
         * Ensures root command ID is matched by the property of the event which is created as
         * a reaction to another event.
         *
         * <p> Two events are expected to be found in the {@linkplain EventStore} created by
         * different aggregates:
         * <ol>
         *     <li>{@link io.spine.server.event.given.EventRootCommandIdTestEnv.ProjectAggregate} —
         *     {@link ProjectCreated}
         *     <li>{@link io.spine.server.event.given.EventRootCommandIdTestEnv.TeamAggregate} —
         *     {@link EvTeamProjectAdded} created as a reaction to {@link ProjectCreated}
         * </ol>
         */
        @Test
        @DisplayName("aggregate")
        void aggregate() {
            Command command = command(createProject(projectId(), teamId()));

            postCommand(command);

            List<Event> events = readEvents();
            assertThat(events).hasSize(2);

            Event reaction = events.get(1);
            assertThat(reaction.rootMessage())
                    .isEqualTo(command.messageId());
        }

        /**
         * Ensures root command ID is matched by the property of the event which is created as
         * a reaction to another event.
         *
         * <p> Two events are expected to be found in the {@linkplain EventStore} created by
         * different process managers:
         * <ol>
         *     <li>{@link io.spine.server.event.given.EventRootCommandIdTestEnv.UserSignUpProcessManager}
         *     — {@link EvInvitationAccepted}
         *     <li>{@link io.spine.server.event.given.EventRootCommandIdTestEnv.TeamCreationProcessManager}
         *     — {@link EvTeamMemberAdded} created as a reaction to {@link EvInvitationAccepted}
         * </ol>
         */
        @Test
        @DisplayName("process manager")
        void processManager() {
            Command command = command(acceptInvitation(teamId()));

            postCommand(command);

            List<Event> events = readEvents();
            assertThat(events).hasSize(2);

            Event reaction = events.get(1);
            assertThat(reaction.rootMessage())
                    .isEqualTo(command.messageId());

        }
    }

    @Nested
    @DisplayName("if the old format of `Event` is used")
    class OldFormat {

        @SuppressWarnings("deprecation") // Backward compatibility test.
        @Test
        @DisplayName("match an ID from `root_command_id`")
        void fromRootCommandId() {
            Event.Builder eventBuilder = GivenEvent.arbitrary()
                                                   .toBuilder();
            CommandId commandId = CommandId.generate();
            eventBuilder.getContextBuilder()
                        .clearOrigin()
                        .setRootCommandId(commandId)
                        .setCommandContext(CommandContext.getDefaultInstance());
            Event event = eventBuilder.buildPartial();
            MessageId rootMessage = event.rootMessage();
            assertThat(unpack(rootMessage.getId()))
                    .isEqualTo(commandId);
            String typeUrl = rootMessage.getTypeUrl();
            assertThat(typeUrl)
                    .isNotEmpty();
            assertThrows(IllegalArgumentException.class, () -> TypeUrl.parse(typeUrl));
        }
    }

    private void postCommand(Command command) {
        StreamObserver<Ack> observer = noOpObserver();
        context.commandBus()
               .post(command, observer);
    }

    /**
     * Reads all events from the bounded context event store.
     */
    private List<Event> readEvents() {
        MemoizingObserver<Event> observer = memoizingObserver();
        with(TENANT_ID).run(() -> context.eventBus()
                                         .eventStore()
                                         .read(allEventsQuery(), observer));
        List<Event> results = observer.responses();
        return results;
    }
}
