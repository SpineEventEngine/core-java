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

package io.spine.server.event.given;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.core.React;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.net.EmailAddress;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.EventRootCommandIdTest;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.server.route.EventRoute;
import io.spine.test.event.EvInvitationAccepted;
import io.spine.test.event.EvMember;
import io.spine.test.event.EvMemberInvitation;
import io.spine.test.event.EvTeam;
import io.spine.test.event.EvTeamCreation;
import io.spine.test.event.EvTeamCreationVBuilder;
import io.spine.test.event.EvTeamId;
import io.spine.test.event.EvTeamMemberAdded;
import io.spine.test.event.EvTeamMemberInvited;
import io.spine.test.event.EvTeamProjectAdded;
import io.spine.test.event.EvTeamVBuilder;
import io.spine.test.event.EvUserSignUp;
import io.spine.test.event.EvUserSignUpVBuilder;
import io.spine.test.event.Project;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectId;
import io.spine.test.event.ProjectVBuilder;
import io.spine.test.event.Task;
import io.spine.test.event.TaskAdded;
import io.spine.test.event.command.CreateProject;
import io.spine.test.event.command.EvAcceptInvitation;
import io.spine.test.event.command.EvAddTasks;
import io.spine.test.event.command.EvAddTeamMember;
import io.spine.test.event.command.EvInviteTeamMembers;
import io.spine.testdata.Sample;
import io.spine.testing.client.TestActorRequestFactory;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singleton;

/**
 * @author Mykhailo Drachuk
 */
public class EventRootCommandIdTestEnv {

    public static final TenantId TENANT_ID = tenantId();

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(EventRootCommandIdTest.class, TENANT_ID);

    private EventRootCommandIdTestEnv() {
        // Prevent instantiation.
    }

    public static ProjectId projectId() {
        return ((ProjectId.Builder) Sample.builderForType(ProjectId.class))
                .build();
    }

    public static EvTeamId teamId() {
        return ((EvTeamId.Builder) Sample.builderForType(EvTeamId.class))
                .build();
    }

    private static TenantId tenantId() {
        String value = EventRootCommandIdTestEnv.class.getName();
        TenantId id = TenantId.newBuilder()
                                    .setValue(value)
                                    .build();
        return id;
    }

    public static Command command(Message message) {
        return requestFactory.createCommand(message);
    }

    public static CreateProject createProject(ProjectId projectId, EvTeamId teamId) {
        checkNotNull(projectId);
        checkNotNull(teamId);

        return CreateProject.newBuilder()
                            .setProjectId(projectId)
                            .setTeamId(teamId)
                            .build();
    }

    public static EvAddTasks addTasks(ProjectId id, int count) {
        checkNotNull(id);

        EvAddTasks.Builder builder = EvAddTasks.newBuilder();
        for (int i = 0; i < count; i++) {
            Task task = (Task) Sample.builderForType(Task.class)
                                           .build();
            builder.addTask(task);
        }

        return builder.setProjectId(id)
                      .build();
    }

    public static EvAddTeamMember addTeamMember(EvTeamId teamId) {
        checkNotNull(teamId);

        return ((EvAddTeamMember.Builder) Sample.builderForType(EvAddTeamMember.class))
                .setTeamId(teamId)
                .build();
    }

    public static EvAcceptInvitation acceptInvitation(EvTeamId teamId) {
        checkNotNull(teamId);

        EvMemberInvitation invitation = memberInvitation(teamId);
        return ((EvAcceptInvitation.Builder) Sample.builderForType(EvAcceptInvitation.class))
                .setInvitation(invitation)
                .build();
    }

    public static EvInviteTeamMembers inviteTeamMembers(EvTeamId teamId, int count) {
        checkNotNull(teamId);

        EvInviteTeamMembers.Builder builder = EvInviteTeamMembers.newBuilder();
        for (int i = 0; i < count; i++) {
            EmailAddress task = (EmailAddress) Sample.builderForType(EmailAddress.class)
                                                           .build();
            builder.addEmail(task);
        }

        return builder.setTeamId(teamId)
                      .build();
    }

    private static EvMemberInvitation memberInvitation(EvTeamId teamId) {
        return ((EvMemberInvitation.Builder) Sample.builderForType(EvMemberInvitation.class))
                .setTeamId(teamId)
                .build();
    }

    /**
     * Creates a new {@link EventStreamQuery} without any filters.
     */
    public static EventStreamQuery allEventsQuery() {
        return EventStreamQuery.newBuilder()
                               .build();
    }

    public static class ProjectAggregateRepository
            extends AggregateRepository<ProjectId, ProjectAggregate> {
    }

    /**
     * Routes the {@link ProjectCreated} event to the {@link TeamAggregate} the project belongs to.
     * This is done for the purposes of the
     * {@linkplain EventRootCommandIdTest.MatchExternalEventHandledBy#aggregate()} test.
     */
    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    public static class TeamAggregateRepository
            extends AggregateRepository<EvTeamId, TeamAggregate> {

        public TeamAggregateRepository() {
            getEventRouting()
                    .route(ProjectCreated.class,
                           new EventRoute<EvTeamId, ProjectCreated>() {
                               private static final long serialVersionUID = 0L;

                               @Override
                               public Set<EvTeamId> apply(ProjectCreated msg, EventContext ctx) {
                                   return singleton(msg.getTeamId());
                               }
                           });
        }
    }

    /**
     * Routes the {@link EvInvitationAccepted} event to the {@link TeamCreationProcessManager} which
     * created the invitation. This is done for the purposes of the
     * {@linkplain EventRootCommandIdTest.MatchExternalEventHandledBy#processManager()} test.
     */
    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    public static class TeamCreationRepository
            extends ProcessManagerRepository<EvTeamId, TeamCreationProcessManager, EvTeamCreation> {

        public TeamCreationRepository() {
            getEventRouting()
                    .route(EvInvitationAccepted.class,
                           new EventRoute<EvTeamId, EvInvitationAccepted>() {
                               private static final long serialVersionUID = 0L;

                               @Override
                               public Set<EvTeamId> apply(EvInvitationAccepted msg,
                                                          EventContext ctx) {
                                   return singleton(msg.getInvitation()
                                                       .getTeamId());
                               }
                           });
        }
    }

    public static class UserSignUpRepository
            extends ProcessManagerRepository<UserId, UserSignUpProcessManager, EvUserSignUp> {
    }

    static class ProjectAggregate extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        private ProjectAggregate(ProjectId id) {
            super(id);
        }

        private static ProjectCreated projectCreated(ProjectId projectId) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(projectId)
                                 .build();
        }

        private static TaskAdded taskAdded(ProjectId projectId, Task task) {
            return TaskAdded.newBuilder()
                            .setProjectId(projectId)
                            .setTask(task)
                            .build();
        }

        @Assign
        ProjectCreated on(CreateProject command, CommandContext ctx) {
            ProjectCreated event = projectCreated(command.getProjectId());
            return event;
        }

        @Assign
        List<TaskAdded> on(EvAddTasks command, CommandContext ctx) {
            ImmutableList.Builder<TaskAdded> events = ImmutableList.builder();

            for (Task task : command.getTaskList()) {
                TaskAdded event = taskAdded(command.getProjectId(), task);
                events.add(event);
            }

            return events.build();
        }

        @Apply
        private void event(ProjectCreated event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.CREATED);
        }

        @Apply
        private void event(TaskAdded event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .addTask(event.getTask());
        }
    }

    static class TeamAggregate extends Aggregate<EvTeamId, EvTeam, EvTeamVBuilder> {

        private TeamAggregate(EvTeamId id) {
            super(id);
        }

        @React
        EvTeamProjectAdded on(ProjectCreated command, EventContext ctx) {
            EvTeamProjectAdded event = projectAdded(command);
            return event;
        }

        @Apply
        private void event(EvTeamProjectAdded event) {
            getBuilder()
                    .setId(event.getTeamId())
                    .addProjectId(event.getProjectId());
        }

        private static EvTeamProjectAdded projectAdded(ProjectCreated command) {
            return EvTeamProjectAdded.newBuilder()
                                     .setProjectId(command.getProjectId())
                                     .build();
        }
    }

    static class TeamCreationProcessManager
            extends ProcessManager<EvTeamId, EvTeamCreation, EvTeamCreationVBuilder> {

        private TeamCreationProcessManager(EvTeamId id) {
            super(id);
        }

        @Assign
        EvTeamMemberAdded on(EvAddTeamMember command, CommandContext ctx) {
            getBuilder().addMember(command.getMember());

            EvTeamMemberAdded event = memberAdded(command.getMember());
            return event;
        }

        @Assign
        List<EvTeamMemberInvited> on(EvInviteTeamMembers command, CommandContext ctx) {
            ImmutableList.Builder<EvTeamMemberInvited> events = ImmutableList.builder();

            for (EmailAddress email : command.getEmailList()) {

                EvMemberInvitation invitation = memberInvitation(email);
                getBuilder().addInvitation(invitation);

                EvTeamMemberInvited event = teamMemberInvited(email);
                events.add(event);
            }

            return events.build();
        }

        @React
        EvTeamMemberAdded on(EvInvitationAccepted event, EventContext ctx) {
            EvMember member = member(event.getUserId());
            EvTeamMemberAdded newEvent = memberAdded(member);
            return newEvent;
        }

        private EvTeamMemberAdded memberAdded(EvMember member) {
            return EvTeamMemberAdded.newBuilder()
                                    .setTeamId(getId())
                                    .setMember(member)
                                    .build();
        }

        private EvTeamMemberInvited teamMemberInvited(EmailAddress email) {
            return EvTeamMemberInvited.newBuilder()
                                      .setTeamId(getId())
                                      .setEmail(email)
                                      .build();
        }

        private static EvMemberInvitation memberInvitation(EmailAddress email) {
            return EvMemberInvitation.newBuilder()
                                     .setEmail(email)
                                     .build();
        }

        private static EvMember member(UserId userId) {
            return ((EvMember.Builder) Sample.builderForType(EvMember.class))
                    .setUserId(userId)
                    .build();
        }
    }

    static class UserSignUpProcessManager
            extends ProcessManager<UserId, EvUserSignUp, EvUserSignUpVBuilder> {

        private UserSignUpProcessManager(UserId id) {
            super(id);
        }

        @Assign
        EvInvitationAccepted on(EvAcceptInvitation command, CommandContext ctx) {
            getBuilder().setInvitation(command.getInvitation());
            EvInvitationAccepted event = invitationAccepted(command.getInvitation());
            return event;
        }

        private EvInvitationAccepted invitationAccepted(EvMemberInvitation invitation) {
            return EvInvitationAccepted.newBuilder()
                                       .setInvitation(invitation)
                                       .setUserId(getId())
                                       .build();
        }
    }
}
