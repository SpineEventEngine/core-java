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

package io.spine.server.event;

import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.grpc.MemoizingObserver;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.event.given.EventRootCommandIdTestEnv.ProjectAggregateRepository;
import io.spine.server.event.given.EventRootCommandIdTestEnv.TeamAggregateRepository;
import io.spine.server.event.given.EventRootCommandIdTestEnv.TeamCreationRepository;
import io.spine.server.event.given.EventRootCommandIdTestEnv.UserSignUpRepository;
import io.spine.server.event.store.EventStore;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.test.event.EvInvitationAccepted;
import io.spine.test.event.EvTeamMemberAdded;
import io.spine.test.event.EvTeamProjectAdded;
import io.spine.test.event.ProjectCreated;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.core.Events.getRootCommandId;
import static io.spine.grpc.StreamObservers.noOpObserver;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Event root CommandId should")
public class EventRootCommandIdTest {

    private BoundedContext boundedContext;

    @BeforeEach
    void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();
        ProjectAggregateRepository projectRepository = new ProjectAggregateRepository();
        TeamAggregateRepository teamRepository = new TeamAggregateRepository();
        TeamCreationRepository teamCreationRepository = new TeamCreationRepository();
        UserSignUpRepository userSignUpRepository = new UserSignUpRepository();

        boundedContext.register(projectRepository);
        boundedContext.register(teamRepository);
        boundedContext.register(teamCreationRepository);
        boundedContext.register(userSignUpRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        boundedContext.close();
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
            assertEquals(command.getId(), getRootCommandId(events.get(0)));
        }

        @Test
        @DisplayName("aggregate in case command returns multiple events")
        void aggregateForMultipleEvents() {
            Command command = command(addTasks(projectId(), 3));

            postCommand(command);

            List<Event> events = readEvents();
            assertThat(events).hasSize(3);
            assertEquals(command.getId(), getRootCommandId(events.get(0)));
            assertEquals(command.getId(), getRootCommandId(events.get(1)));
            assertEquals(command.getId(), getRootCommandId(events.get(2)));
        }

        @Test
        @DisplayName("process manager")
        void processManager() {
            Command command = command(addTeamMember(teamId()));

            postCommand(command);

            List<Event> events = readEvents();
            assertThat(events).hasSize(1);

            Event event = events.get(0);
            assertEquals(command.getId(), getRootCommandId(event));
        }

        @Test
        @DisplayName("process manager in case command returns multiple events")
        void processManagerForMultipleEvents() {
            Command command = command(inviteTeamMembers(teamId(), 3));

            postCommand(command);

            List<Event> events = readEvents();
            assertThat(events).hasSize(3);
            assertEquals(command.getId(), getRootCommandId(events.get(0)));
            assertEquals(command.getId(), getRootCommandId(events.get(1)));
            assertEquals(command.getId(), getRootCommandId(events.get(2)));
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
         *     {@link ProjectCreated}</li>
         *     <li>{@link io.spine.server.event.given.EventRootCommandIdTestEnv.TeamAggregate} —
         *     {@link EvTeamProjectAdded} created as a reaction to {@link ProjectCreated}</li>
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
            assertEquals(command.getId(), getRootCommandId(reaction));
        }

        /**
         * Ensures root command ID is matched by the property of the event which is created as
         * a reaction to another event.
         *
         * <p> Two events are expected to be found in the {@linkplain EventStore} created by
         * different process managers:
         * <ol>
         *     <li>{@link io.spine.server.event.given.EventRootCommandIdTestEnv.UserSignUpProcessManager}
         *     — {@link EvInvitationAccepted}</li>
         *     <li>{@link io.spine.server.event.given.EventRootCommandIdTestEnv.TeamCreationProcessManager}
         *     — {@link EvTeamMemberAdded} created as a reaction to {@link EvInvitationAccepted}
         *     </li>
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
            assertEquals(command.getId(), getRootCommandId(reaction));
        }
    }

    private void postCommand(Command command) {
        StreamObserver<Ack> observer = noOpObserver();
        boundedContext.getCommandBus()
                      .post(command, observer);
    }

    /**
     * Reads all events from the bounded context event store.
     */
    private List<Event> readEvents() {
        MemoizingObserver<Event> observer = StreamObservers.memoizingObserver();

        TenantAwareOperation operation = new TenantAwareOperation(TENANT_ID) {
            @Override
            public void run() {
                boundedContext.getEventBus()
                              .getEventStore()
                              .read(allEventsQuery(), observer);
            }
        };
        operation.execute();

        List<Event> results = observer.responses();
        return results;
    }
}
