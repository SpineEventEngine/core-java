/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.sample.server;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.*;
import org.spine3.protobuf.Messages;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.util.Commands;
import org.spine3.util.Events;
import org.spine3.util.Users;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.spine3.protobuf.Messages.toAny;

@SuppressWarnings({"InstanceMethodNamingConvention", "MethodMayBeStatic", "MagicNumber", "DuplicateStringLiteralInspection"})
public class InMemoryMessageJournalShould {

    private UserId userId;
    private ProjectId projectId;

    private List<CommandRequest> commandRequests;
    private List<EventRecord> eventRecords;

    private InMemoryMessageJournal<Message, CommandRequest> commandStorage;
    private InMemoryMessageJournal<Message, EventRecord> eventStorage;

    @Before
    public void setUp() {
        userId = Users.createId("user@testing-in-memory-storage.org");
        projectId = ProjectId.newBuilder().setId("project_id").build();

        commandRequests = newArrayList(createProject(), startProject());
        eventRecords = newArrayList(projectCreated(), projectStarted());

        commandStorage = InMemoryMessageJournal.newInstance(CommandRequest.class);
        eventStorage = InMemoryMessageJournal.newInstance(EventRecord.class);
    }

    @Test
    public void save_and_read_commands_by_id() {

        for (CommandRequest request : commandRequests) {
            commandStorage.save(projectId, request);
        }

        final List<CommandRequest> messages = commandStorage.getById(projectId);

        assertEquals(commandRequests, messages);
    }

    @Test
    public void save_and_read_events_by_id() {

        for (EventRecord record : eventRecords) {
            eventStorage.save(projectId, record);
        }

        final List<EventRecord> messages = eventStorage.getById(projectId);

        assertEquals(eventRecords, messages);
    }

    @Test
    public void save_and_read_all_commands() {

        for (CommandRequest request : commandRequests) {
            commandStorage.save(projectId, request);
        }

        final List<CommandRequest> messages = commandStorage.getAll();

        assertEquals(commandRequests, messages);
    }

    @Test
    public void save_and_read_all_events() {

        for (EventRecord record : eventRecords) {
            eventStorage.save(projectId, record);
        }

        final List<EventRecord> messages = eventStorage.getAll();

        assertEquals(eventRecords, messages);
    }

    private CommandRequest createProject() {

        final CreateProject command = CreateProject.newBuilder().setProjectId(projectId).build();
        final Any any = toAny(command);
        CommandRequest result = CommandRequest.newBuilder().setCommand(any).build();
        return result;
    }

    private CommandRequest startProject() {

        final StartProject command = StartProject.newBuilder().setProjectId(projectId).build();
        final Any any = toAny(command);
        CommandRequest result = CommandRequest.newBuilder().setCommand(any).build();
        return result;
    }

    private EventRecord projectCreated() {

        final ProjectCreated event = ProjectCreated.newBuilder().setProjectId(projectId).build();
        final EventContext context = getEventContext();
        final EventRecord.Builder builder = EventRecord.newBuilder().setContext(context).setEvent(toAny(event));
        return builder.build();
    }

    private EventRecord projectStarted() {

        final ProjectStarted event = ProjectStarted.newBuilder().setProjectId(projectId).build();
        final EventContext context = getEventContext();
        final EventRecord.Builder builder = EventRecord.newBuilder().setContext(context).setEvent(toAny(event));
        return builder.build();
    }

    private EventContext getEventContext() {
        final CommandId commandId = Commands.generateId(userId);
        final EventId eventId = Events.generateId(commandId);

        final EventContext.Builder builder = EventContext.newBuilder()
                .setEventId(eventId)
                .setAggregateId(Messages.toAny(projectId));

        return builder.build();
    }
}
