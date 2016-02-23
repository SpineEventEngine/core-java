/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage;

import com.google.protobuf.Any;
import com.google.protobuf.util.TimeUtil;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Error;
import org.spine3.base.Failure;
import org.spine3.server.aggregate.AggregateId;
import org.spine3.testdata.TestContextFactory;
import org.spine3.type.TypeName;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.*;
import static org.spine3.base.Commands.generateId;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testdata.TestCommands.createProject;
import static org.spine3.testdata.TestEventMessageFactory.projectCreatedEventAny;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public abstract class CommandStorageShould extends AbstractStorageShould<CommandId, CommandStorageRecord> {

    private CommandStorage storage;

    @SuppressWarnings("FieldCanBeLocal")
    private CommandStorageRecord record;

    private CommandId id;

    @Before
    public void setUpCommandStorageTest() {
        storage = getStorage();
    }

    @Override
    protected abstract CommandStorage getStorage();

    @Override
    protected CommandStorageRecord newStorageRecord() {
        return newCommandStorageRecord();
    }

    @Override
    protected CommandId newId() {
        return generateId();
    }

    @Test
    public void override_read_method_and_do_not_throw_exception() {
        try {
            storage.read(CommandId.getDefaultInstance());
        } catch (UnsupportedOperationException e) {
            fail("read() method must be overridden if you want to use these tests.");
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void store_and_read_command() {
        final Command command = createProject();
        final CommandId id = command.getContext().getCommandId();
        storage.store(command, AggregateId.fromCommand(command));

        final CommandStorageRecord record = storage.read(id);

        assertEquals(command.getMessage(), record.getMessage());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void set_ok_command_status() {
        givenNewRecord();

        storage.setOkStatus(id);

        final CommandStorageRecord actual = storage.read(id);
        assertEquals(CommandStatus.OK, actual.getStatus());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void set_error_command_status() {
        givenNewRecord();
        final Error error = newError();

        storage.updateStatus(id, error);

        final CommandStorageRecord actual = storage.read(id);
        assertEquals(CommandStatus.ERROR, actual.getStatus());
        assertEquals(error, actual.getError());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void set_failure_command_status() {
        givenNewRecord();
        final Failure failure = newFailure();

        storage.updateStatus(id, failure);

        final CommandStorageRecord actual = storage.read(id);
        assertEquals(CommandStatus.FAILURE, actual.getStatus());
        assertEquals(failure, actual.getFailure());
    }

    private void givenNewRecord() {
        record = newCommandStorageRecord();
        id = record.getContext().getCommandId();
        storage.write(id, record);
    }

    private static CommandStorageRecord newCommandStorageRecord() {
        final String aggregateIdString = newUuid();
        final AggregateId<String> aggregateId = AggregateId.of(aggregateIdString);
        final Any command = toAny(createProject());
        final TypeName commandType = TypeName.ofEnclosed(command);
        final CommandContext context = TestContextFactory.createCommandContext();
        final CommandStorageRecord.Builder builder = CommandStorageRecord.newBuilder()
                .setTimestamp(getCurrentTime())
                .setCommandType(commandType.nameOnly())
                .setCommandId(context.getCommandId().getUuid())
                .setAggregateIdType(aggregateId.getShortTypeName())
                .setAggregateId(aggregateIdString)
                .setMessage(command)
                .setContext(context);
        return builder.build();
    }

    private static Error newError() {
        return Error.newBuilder()
                .setType("error type 123")
                .setCode(5)
                .setMessage("error message 123")
                .setStacktrace("stacktrace")
                .build();
    }

    private static Failure newFailure() {
        return Failure.newBuilder()
                .setInstance(projectCreatedEventAny())
                .setStacktrace("failure stacktrace")
                .setTimestamp(TimeUtil.getCurrentTime())
                .build();
    }
}
