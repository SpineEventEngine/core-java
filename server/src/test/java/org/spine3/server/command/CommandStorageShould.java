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

package org.spine3.server.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
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
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Values;
import org.spine3.server.command.storage.CommandStorageRecord;
import org.spine3.server.storage.AbstractStorageShould;
import org.spine3.test.Tests;
import org.spine3.test.command.CreateProject;
import org.spine3.test.command.ProjectId;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.CommandStatus.ERROR;
import static org.spine3.base.CommandStatus.FAILURE;
import static org.spine3.base.CommandStatus.OK;
import static org.spine3.base.CommandStatus.RECEIVED;
import static org.spine3.base.CommandStatus.SCHEDULED;
import static org.spine3.base.Commands.generateId;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.base.Stringifiers.idToString;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.protobuf.Timestamps.getCurrentTime;
import static org.spine3.protobuf.TypeUrl.ofEnclosed;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;
import static org.spine3.validate.Validate.isDefault;
import static org.spine3.validate.Validate.isNotDefault;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public abstract class CommandStorageShould
        extends AbstractStorageShould<CommandId, CommandStorageRecord, CommandStorage> {

    private static final Error defaultError = Error.getDefaultInstance();
    private static final Failure defaultFailure = Failure.getDefaultInstance();

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
    protected CommandStorageRecord newStorageRecord() {
        final Any command = AnyPacker.pack(Given.Command.createProject());
        final String commandType = ofEnclosed(command).getTypeName();
        final CommandContext context = createCommandContext();
        final CommandStorageRecord.Builder builder = CommandStorageRecord.newBuilder()
                .setTimestamp(getCurrentTime())
                .setCommandType(commandType)
                .setCommandId(idToString(context.getCommandId()))
                .setStatus(RECEIVED)
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

    /*
     * Storing and loading tests.
     ****************************/

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
    @Test
    public void store_and_read_command() {
        final Command command = Given.Command.createProject();
        final CommandId commandId = getId(command);

        storage.store(command);
        final CommandStorageRecord record = storage.read(commandId).get();

        checkRecord(record, command, RECEIVED);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
    @Test
    public void store_command_with_error() {
        final Command command = Given.Command.createProject();
        final CommandId commandId = getId(command);
        final Error error = newError();

        storage.store(command, error);
        final CommandStorageRecord record = storage.read(commandId).get();

        checkRecord(record, command, ERROR);
        assertEquals(error, record.getError());
    }

    @Test
    public void store_command_with_error_and_generate_ID_if_needed() {
        final Command command = Commands.createCommand(Given.CommandMessage.createProject(),
                                                       CommandContext.getDefaultInstance());
        final Error error = newError();

        storage.store(command, error);
        final List<CommandStorageRecord> records = Lists.newArrayList(storage.read(ERROR));

        assertEquals(1, records.size());
        assertFalse(records.get(0)
                           .getCommandId()
                           .trim()
                           .isEmpty());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
    @Test
    public void store_command_with_status() {
        final Command command = Given.Command.createProject();
        final CommandId commandId = getId(command);
        final CommandStatus status = SCHEDULED;

        storage.store(command, status);
        final CommandStorageRecord record = storage.read(commandId).get();

        checkRecord(record, command, status);
    }

    @Test
    public void load_commands_by_status() {
        final List<Command> commands = ImmutableList.of(Given.Command.createProject(),
                                                        Given.Command.addTask(),
                                                        Given.Command.startProject());
        final CommandStatus status = SCHEDULED;

        store(commands, status);
        // store an extra command with another status
        storage.store(Given.Command.createProject(), ERROR);

        final Iterator<Command> iterator = storage.iterator(status);
        final List<Command> actualCommands = newArrayList(iterator);
        assertEquals(commands.size(), actualCommands.size());
        for (Command cmd : actualCommands) {
            assertTrue(commands.contains(cmd));
        }
    }

    /*
     * Update command status tests.
     ******************************/

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
    @Test
    public void set_ok_command_status() {
        givenNewRecord();

        storage.setOkStatus(id);

        final CommandStorageRecord actual = storage.read(id).get();
        assertEquals(OK, actual.getStatus());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
    @Test
    public void set_error_command_status() {
        givenNewRecord();
        final Error error = newError();

        storage.updateStatus(id, error);

        final CommandStorageRecord actual = storage.read(id).get();
        assertEquals(ERROR, actual.getStatus());
        assertEquals(error, actual.getError());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
    @Test
    public void set_failure_command_status() {
        givenNewRecord();
        final Failure failure = newFailure();

        storage.updateStatus(id, failure);

        final CommandStorageRecord actual = storage.read(id).get();
        assertEquals(FAILURE, actual.getStatus());
        assertEquals(failure, actual.getFailure());
    }

    /*
     * Conversion tests.
     *******************/

    @Test
    public void convert_cmd_to_record() {
        final Command command = Given.Command.createProject();
        final CommandStatus status = RECEIVED;

        final CommandStorageRecord record = CommandStorage.newRecordBuilder(command, status).build();

        checkRecord(record, command, status);
    }

    @Test
    public void convert_cmd_to_record_and_set_empty_target_id_if_message_has_no_id_field() {
        final StringValue message = StringValue.getDefaultInstance();
        final Command command = Commands.createCommand(message, CommandContext.getDefaultInstance());
        final CommandStorageRecord record = CommandStorage.newRecordBuilder(command, RECEIVED).build();

        assertEquals("", record.getTargetId());
        assertEquals("", record.getTargetIdType());
    }

    /*
     * Check that exception is thrown if try to pass null to methods.
     **************************************************************/

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_store_null() {
        storage.store(Tests.<Command>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_set_OK_status_by_null_ID() {
        storage.setOkStatus(Tests.<CommandId>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_set_error_status_by_null_ID() {
        storage.updateStatus(Tests.<CommandId>nullRef(), defaultError);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_set_failure_status_by_null_ID() {
        storage.updateStatus(Tests.<CommandId>nullRef(), defaultFailure);
    }

    /*
     * Check that exception is thrown if try to use closed storage.
     **************************************************************/

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_store_cmd_to_closed_storage() {
        close(storage);
        storage.store(Given.Command.createProject());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_store_cmd_with_error_to_closed_storage() {
        close(storage);
        storage.store(Given.Command.createProject(), newError());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_store_cmd_with_status_to_closed_storage() {
        close(storage);
        storage.store(Given.Command.createProject(), OK);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_load_commands_by_status_from_closed_storage() {
        close(storage);
        storage.iterator(OK);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_set_OK_status_using_closed_storage() {
        close(storage);
        storage.setOkStatus(generateId());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_set_ERROR_status_using_closed_storage() {
        close(storage);
        storage.updateStatus(generateId(), newError());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_set_FAILURE_status_using_closed_storage() {
        close(storage);
        storage.updateStatus(generateId(), newFailure());
    }

    /*
     * Utils.
     **************************************************************/

    private void store(Iterable<Command> commands, CommandStatus status) {
        for (Command cmd : commands) {
            storage.store(cmd, status);
        }
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
                      .setInstance(AnyPacker.pack(Values.newStringValue("newFailure")))
                      .setStacktrace("failure stacktrace")
                      .setTimestamp(getCurrentTime())
                      .build();
    }

    private static void checkRecord(CommandStorageRecord record, Command cmd, CommandStatus statusExpected) {
        final CommandContext context = cmd.getContext();
        final CommandId commandId = context.getCommandId();
        final CreateProject message = unpack(cmd.getMessage());
        assertEquals(cmd.getMessage(), record.getMessage());
        assertTrue(record.getTimestamp().getSeconds() > 0);
        assertEquals(message.getClass().getSimpleName(), record.getCommandType());
        assertEquals(idToString(commandId), record.getCommandId());
        assertEquals(statusExpected, record.getStatus());
        assertEquals(ProjectId.class.getName(), record.getTargetIdType());
        assertEquals(message.getProjectId().getId(), record.getTargetId());
        assertEquals(context, record.getContext());
        switch (statusExpected) {
            case RECEIVED:
            case OK:
            case SCHEDULED:
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
