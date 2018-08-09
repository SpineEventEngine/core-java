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

package io.spine.server.procman.given.pm;

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.core.React;
import io.spine.server.command.Assign;
import io.spine.server.command.Command;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.rejection.StandardRejections;
import io.spine.server.procman.ProcessManager;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmAddTask;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.command.PmStartProject;
import io.spine.test.procman.event.PmNotificationSent;
import io.spine.test.procman.event.PmProjectCreated;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.event.PmTaskAdded;
import io.spine.testdata.Sample;
import io.spine.validate.AnyVBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;

/**
 * A test Process Manager which remembers past message as its state.
 */
public class TestProcessManager
        extends ProcessManager<ProjectId, Any, AnyVBuilder> {

    public TestProcessManager(ProjectId id) {
        super(id);
    }

    /**
     * Injects the passed CommandBus instance via Reflection since
     * {@link #setCommandBus(CommandBus)} is package-private and this
     * test environment class is outside of the parent's class package.
     */
    public void injectCommandBus(CommandBus commandBus) {
        try {
            Method method = ProcessManager.class.getDeclaredMethod("setCommandBus",
                                                                   CommandBus.class);
            method.setAccessible(true);
            method.invoke(this, commandBus);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw illegalStateWithCauseOf(e);
        }
    }

    /*
     * Handled commands
     ********************/

    @Assign
    PmProjectCreated handle(PmCreateProject command, CommandContext ignored) {
        getBuilder().mergeFrom(pack(command));
        return ((PmProjectCreated.Builder) Sample.builderForType(PmProjectCreated.class))
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    PmTaskAdded handle(PmAddTask command, CommandContext ignored) {
        getBuilder().mergeFrom(pack(command));
        return ((PmTaskAdded.Builder) Sample.builderForType(PmTaskAdded.class))
                .setProjectId(command.getProjectId())
                .build();
    }

    @Command
    PmAddTask transform(PmStartProject command, CommandContext context) {
        getBuilder().mergeFrom(pack(command));

        PmAddTask addTask = ((PmAddTask.Builder)
                Sample.builderForType(PmAddTask.class))
                .setProjectId(command.getProjectId())
                .build();
        return addTask;
    }

    /*
     * Reactions on events
     ************************/

    @React
    public Empty on(PmProjectCreated event, EventContext ignored) {
        getBuilder().mergeFrom(pack(event));
        return Empty.getDefaultInstance();
    }

    @React
    public Empty on(PmTaskAdded event, EventContext ignored) {
        getBuilder().mergeFrom(pack(event));
        return Empty.getDefaultInstance();
    }

    @React
    public Message on(PmProjectStarted event, EventContext ignored) {
        getBuilder().mergeFrom(pack(event));
        return Sample.messageOfType(PmNotificationSent.class);
    }


    /*
     * Reactions on rejections
     **************************/

    @React
    Empty on(StandardRejections.EntityAlreadyArchived rejection, PmAddTask command) {
        getBuilder().mergeFrom(pack(command));
        return Empty.getDefaultInstance();
    }

    @React
    Empty on(StandardRejections.EntityAlreadyArchived rejection) {
        getBuilder().mergeFrom(pack(rejection));
        return Empty.getDefaultInstance();
    }
}
