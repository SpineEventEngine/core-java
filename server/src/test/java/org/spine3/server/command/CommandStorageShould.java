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
import org.spine3.protobuf.TypeName;
import org.spine3.protobuf.Values;
import org.spine3.server.storage.AbstractStorageShould;
import org.spine3.test.Tests;
import org.spine3.test.command.CreateProject;

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
import static org.spine3.base.Stringifiers.idToString;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;
import static org.spine3.server.command.Given.Command.createProject;
import static org.spine3.validate.Validate.isDefault;
import static org.spine3.validate.Validate.isNotDefault;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public abstract class CommandStorageShould
        extends AbstractStorageShould<CommandId, CommandRecord, CommandStorage> {

    private static final Error defaultError = Error.getDefaultInstance();
    private static final Failure defaultFailure = Failure.getDefaultInstance();

    private CommandStorage storage;

    @SuppressWarnings("FieldCanBeLocal")
    private CommandRecord record;

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
    protected CommandRecord newStorageRecord() {
        final Command command = createProject();
        final String commandType = TypeName.ofCommand(command);

        final CommandRecord.Builder builder =
                CommandRecord.newBuilder()
                             .setCommandType(commandType)
                             .setCommandId(command.getContext().getCommandId())
                             .setCommand(command)
                             .setTimestamp(getCurrentTime())
                             .setStatus(ProcessingStatus.newBuilder()
                                                        .setCode(RECEIVED));
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
        final Command command = createProject();
        final CommandId commandId = getId(command);

        storage.store(command);
        final CommandRecord record = storage.read(commandId).get();

        checkRecord(record, command, RECEIVED);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
    @Test
    public void store_command_with_error() {
        final Command command = createProject();
        final CommandId commandId = getId(command);
        final Error error = newError();

        storage.store(command, error);
        final CommandRecord record = storage.read(commandId).get();

        checkRecord(record, command, ERROR);
        assertEquals(error, record.getStatus()
                                  .getError());
    }

    @Test
    public void store_command_with_error_and_generate_ID_if_needed() {
        final Command command = Commands.createCommand(Given.CommandMessage.createProjectMessage(),
                                                       CommandContext.getDefaultInstance());
        final Error error = newError();

        storage.store(command, error);
        final List<CommandRecord> records = Lists.newArrayList(storage.read(ERROR));

        assertEquals(1, records.size());
        final String commandIdStr = idToString(records.get(0)
                                                      .getCommandId());
        assertFalse(commandIdStr.isEmpty());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
    @Test
    public void store_command_with_status() {
        final Command command = createProject();
        final CommandId commandId = getId(command);
        final CommandStatus status = SCHEDULED;

        storage.store(command, status);
        final CommandRecord record = storage.read(commandId).get();

        checkRecord(record, command, status);
    }

    @Test
    public void load_commands_by_status() {
        final List<Command> commands = ImmutableList.of(createProject(),
                                                        Given.Command.addTask(),
                                                        Given.Command.startProject());
        final CommandStatus status = SCHEDULED;

        store(commands, status);
        // store an extra command with another status
        storage.store(createProject(), ERROR);

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

        final CommandRecord actual = storage.read(id).get();
        assertEquals(OK, actual.getStatus()
                               .getCode());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
    @Test
    public void set_error_command_status() {
        givenNewRecord();
        final Error error = newError();

        storage.updateStatus(id, error);

        final CommandRecord actual = storage.read(id).get();
        assertEquals(ERROR, actual.getStatus().getCode());
        assertEquals(error, actual.getStatus().getError());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
    @Test
    public void set_failure_command_status() {
        givenNewRecord();
        final Failure failure = newFailure();

        storage.updateStatus(id, failure);

        final CommandRecord actual = storage.read(id).get();
        assertEquals(FAILURE, actual.getStatus()
                                    .getCode());
        assertEquals(failure, actual.getStatus()
                                    .getFailure());
    }

    /*
     * Conversion tests.
     *******************/

    @Test
    public void convert_cmd_to_record() {
        final Command command = createProject();
        final CommandStatus status = RECEIVED;

        final CommandRecord record = CommandStorage.newRecordBuilder(command, status, null).build();

        checkRecord(record, command, status);
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
        storage.store(createProject());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_store_cmd_with_error_to_closed_storage() {
        close(storage);
        storage.store(createProject(), newError());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_store_cmd_with_status_to_closed_storage() {
        close(storage);
        storage.store(createProject(), OK);
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
        id = record.getCommandId();
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
        final Any packedFailureMessage = AnyPacker.pack(Values.newStringValue("newFailure"));
        return Failure.newBuilder()
                      .setMessage(packedFailureMessage)
                      .setStacktrace("failure stacktrace")
                      .setTimestamp(getCurrentTime())
                      .build();
    }

    private static void checkRecord(CommandRecord record,
                                    Command cmd,
                                    CommandStatus statusExpected) {
        final CommandContext context = cmd.getContext();
        final CommandId commandId = context.getCommandId();
        final CreateProject message = unpack(cmd.getMessage());
        assertEquals(cmd.getMessage(), record.getCommand()
                                             .getMessage());
        assertTrue(record.getTimestamp()
                         .getSeconds() > 0);
        assertEquals(message.getClass()
                            .getSimpleName(), record.getCommandType());
        assertEquals(commandId, record.getCommandId());
        assertEquals(statusExpected, record.getStatus()
                                           .getCode());
        assertEquals(context, record.getCommand()
                                    .getContext());
        switch (statusExpected) {
            case RECEIVED:
            case OK:
            case SCHEDULED:
                assertTrue(isDefault(record.getStatus()
                                           .getError()));
                assertTrue(isDefault(record.getStatus()
                                           .getFailure()));
                break;
            case ERROR:
                assertTrue(isNotDefault(record.getStatus()
                                              .getError()));
                break;
            case FAILURE:
                assertTrue(isNotDefault(record.getStatus()
                                              .getFailure()));
                break;
            case UNDEFINED:
            case UNRECOGNIZED:
                break;
        }
    }
}
