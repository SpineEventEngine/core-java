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
import com.google.protobuf.StringValue;
import com.google.protobuf.util.TimeUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.base.Commands;
import org.spine3.base.Error;
import org.spine3.base.Failure;
import org.spine3.server.command.CommandValidator;
import org.spine3.server.entity.GetTargetIdFromCommand;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.CreateProject;
import org.spine3.type.TypeName;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.*;
import static org.spine3.base.CommandStatus.*;
import static org.spine3.base.Commands.generateId;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.testdata.TestCommands.createProjectCmd;
import static org.spine3.testdata.TestEventMessageFactory.projectCreatedEventAny;
import static org.spine3.validate.Validate.isDefault;
import static org.spine3.validate.Validate.isNotDefault;

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

    @After
    public void tearDownCommandStorageTest() {
        close(storage);
    }

    @Override
    protected abstract CommandStorage getStorage();

    @Override
    protected CommandStorageRecord newStorageRecord() {
        final Any command = toAny(createProjectCmd());
        final TypeName commandType = TypeName.ofEnclosed(command);
        final CommandContext context = createCommandContext();
        final CommandStorageRecord.Builder builder = CommandStorageRecord.newBuilder()
                .setTimestamp(getCurrentTime())
                .setCommandType(commandType.nameOnly())
                .setCommandId(idToString(context.getCommandId()))
                .setTargetId(newUuid())
                .setTargetIdType(String.class.getName())
                .setMessage(command)
                .setContext(context);
        return builder.build();
    }

    @Override
    protected CommandId newId() {
        return generateId();
    }

    @Test
    public void store_and_read_command() {
        final Command command = createProjectCmd();
        final CommandId commandId = command.getContext().getCommandId();

        storage.store(command);
        final CommandStorageRecord record = storage.read(commandId);

        checkRecord(command, record, RECEIVED);
    }

    @Test
    public void store_command_with_error_status() {
        final Command command = createProjectCmd();
        final CommandId commandId = command.getContext().getCommandId();
        final Error error = newError();

        storage.store(command, error);

        final CommandStorageRecord record = storage.read(commandId);

        checkRecord(command, record, ERROR);
        assertEquals(error, record.getError());
    }

    @Test
    public void set_ok_command_status() {
        givenNewRecord();

        storage.setOkStatus(id);

        final CommandStorageRecord actual = storage.read(id);
        assertEquals(OK, actual.getStatus());
    }

    @Test
    public void set_error_command_status() {
        givenNewRecord();
        final Error error = newError();

        storage.updateStatus(id, error);

        final CommandStorageRecord actual = storage.read(id);
        assertEquals(ERROR, actual.getStatus());
        assertEquals(error, actual.getError());
    }

    @Test
    public void set_failure_command_status() {
        givenNewRecord();
        final Failure failure = newFailure();

        storage.updateStatus(id, failure);

        final CommandStorageRecord actual = storage.read(id);
        assertEquals(FAILURE, actual.getStatus());
        assertEquals(failure, actual.getFailure());
    }

    @Test
    public void convert_cmd_to_record() {
        final Command command = createProjectCmd();

        final CommandStorageRecord record = CommandStorage.toStorageRecord(command);

        checkRecord(command, record, RECEIVED);
    }

    @Test
    public void convert_cmd_to_record_and_set_empty_target_id_if_message_has_no_id_field() {
        final StringValue message = StringValue.getDefaultInstance();
        final Command command = Commands.create(message, CommandContext.getDefaultInstance());
        final CommandStorageRecord record = CommandStorage.toStorageRecord(command);

        assertEquals("", record.getTargetId());
        assertEquals("", record.getTargetIdType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void check_command_and_throw_exception_if_it_is_invalid() {
        final Command command = Commands.create(StringValue.getDefaultInstance(), CommandContext.getDefaultInstance());
        CommandValidator.checkCommand(command);
    }

    @Test
    public void check_command_and_do_not_throw_exception_if_it_is_valid() {
        final Command command = createProjectCmd();
        CommandValidator.checkCommand(command);
    }

    @Test
    public void return_null_when_fail_to_get_id_from_command_message_which_has_no_id_field() {
        final Object id = GetTargetIdFromCommand.asNullableObject(StringValue.getDefaultInstance());
        assertNull(id);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_store_null() {
        //noinspection ConstantConditions
        storage.store(null);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_set_OK_status_by_null_ID() {
        //noinspection ConstantConditions
        storage.setOkStatus(null);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_set_error_status_by_null_ID() {
        //noinspection ConstantConditions
        storage.updateStatus(null, Error.getDefaultInstance());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_set_failure_status_by_null_ID() {
        //noinspection ConstantConditions
        storage.updateStatus(null, Failure.getDefaultInstance());
    }

    private void givenNewRecord() {
        record = newStorageRecord();
        id = record.getContext().getCommandId();
        storage.write(id, record);
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

    private static void checkRecord(
            Command cmd,
            CommandStorageRecord record,
            CommandStatus statusExpected) {
        final CommandContext context = cmd.getContext();
        final CommandId commandId = context.getCommandId();
        final CreateProject message = fromAny(cmd.getMessage());

        assertEquals(cmd.getMessage(), record.getMessage());
        assertEquals(context.getTimestamp(), record.getTimestamp());
        assertEquals(message.getClass().getSimpleName(), record.getCommandType());
        assertEquals(idToString(commandId), record.getCommandId());
        assertEquals(statusExpected, record.getStatus());
        assertEquals(ProjectId.class.getName(), record.getTargetIdType());
        assertEquals(message.getProjectId().getId(), record.getTargetId());
        assertEquals(context, record.getContext());
        switch (statusExpected) {
            case RECEIVED:
            case OK:
                assertTrue(isDefault(record.getError()));
                assertTrue(isDefault(record.getFailure()));
                break;
            case ERROR:
                assertTrue(isNotDefault(record.getError()));
                break;
            case FAILURE:
                assertTrue(isNotDefault(record.getFailure()));
                break;
            case UNDEFINED:
            case UNRECOGNIZED:
                break;
        }
    }
}
