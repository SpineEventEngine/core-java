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
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventContext;
import org.spine3.base.EventRecord;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.command.StartProject;
import org.spine3.test.project.event.ProjectCreated;
import org.spine3.test.project.event.ProjectStarted;
import org.spine3.testutil.AggregateIdFactory;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testutil.ContextFactory.getEventContext;

@SuppressWarnings({"InstanceMethodNamingConvention", "MethodMayBeStatic", "MagicNumber", "DuplicateStringLiteralInspection"})
public class InMemoryStorageShould {

    private ProjectId projectId;

    private List<CommandRequest> commandRequests;
    private List<EventRecord> eventRecords;

    private InMemoryStorage<CommandRequest> commandStorage;
    private InMemoryStorage<EventRecord> eventStorage;

    @Before
    public void setUp() {

        projectId = AggregateIdFactory.createCommon();

        commandRequests = newArrayList(createProject(), startProject());
        eventRecords = newArrayList(projectCreated(), projectStarted());

        commandStorage = new InMemoryStorage<>(CommandRequest.class);
        eventStorage = new InMemoryStorage<>(EventRecord.class);
    }

    @Test
    public void save_and_read_commands_by_aggregate_id() {

        for (CommandRequest request : commandRequests) {
            commandStorage.save(request);
        }

        final List<CommandRequest> messages = commandStorage.read(CommandRequest.class, projectId);

        assertEquals(commandRequests, messages);
    }

    @Test
    public void save_and_read_events_by_aggregate_id() {

        for (EventRecord record : eventRecords) {
            eventStorage.save(record);
        }

        final List<EventRecord> messages = eventStorage.read(EventRecord.class, projectId);

        assertEquals(eventRecords, messages);
    }

    @Test
    public void save_and_read_all_commands() {

        for (CommandRequest request : commandRequests) {
            commandStorage.save(request);
        }

        final List<CommandRequest> messages = commandStorage.readAll(CommandRequest.class);

        assertEquals(commandRequests, messages);
    }

    @Test
    public void save_and_read_all_events() {

        for (EventRecord record : eventRecords) {
            eventStorage.save(record);
        }

        final List<EventRecord> messages = eventStorage.readAll(EventRecord.class);

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
        final EventContext context = getEventContext(0);
        final EventRecord.Builder builder = EventRecord.newBuilder().setContext(context).setEvent(toAny(event));
        return builder.build();
    }

    private EventRecord projectStarted() {

        final ProjectStarted event = ProjectStarted.newBuilder().setProjectId(projectId).build();
        final EventContext context = getEventContext(0);
        final EventRecord.Builder builder = EventRecord.newBuilder().setContext(context).setEvent(toAny(event));
        return builder.build();
    }
}
