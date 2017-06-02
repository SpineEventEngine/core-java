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

package io.spine.server.commandstore;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import io.spine.base.Command;
import io.spine.base.CommandContext;
import io.spine.base.CommandId;
import io.spine.base.CommandStatus;
import io.spine.base.Commands;
import io.spine.base.Error;
import io.spine.base.Failure;
import io.spine.base.FailureContext;
import io.spine.base.FailureId;
import io.spine.base.Failures;
import io.spine.server.commandbus.CommandRecord;
import io.spine.server.commandbus.Given;
import io.spine.server.commandbus.ProcessingStatus;
import io.spine.server.storage.StorageFactorySwitch;
import io.spine.server.tenant.TenantAwareTest;
import io.spine.test.TestActorRequestFactory;
import io.spine.test.Tests;
import io.spine.type.TypeName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.CommandStatus.ERROR;
import static io.spine.base.CommandStatus.FAILURE;
import static io.spine.base.CommandStatus.OK;
import static io.spine.base.CommandStatus.RECEIVED;
import static io.spine.base.CommandStatus.SCHEDULED;
import static io.spine.base.Commands.generateId;
import static io.spine.base.Identifiers.idToString;
import static io.spine.protobuf.Wrappers.pack;
import static io.spine.server.commandbus.Given.CommandMessage.createProjectMessage;
import static io.spine.server.commandstore.CommandTestUtil.checkRecord;
import static io.spine.server.commandstore.Records.newRecordBuilder;
import static io.spine.server.commandstore.Records.toCommandIterator;
import static io.spine.test.Tests.newTenantUuid;
import static io.spine.time.Time.getCurrentTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ConstantConditions")
public class StorageShould extends TenantAwareTest {

    private static final Error defaultError = Error.getDefaultInstance();
    private static final Failure defaultFailure = Failure.getDefaultInstance();

    private Storage storage;

    private CommandId id;

    @Before
    public void setUpCommandStorageTest() {
        setCurrentTenant(newTenantUuid());
        storage = new Storage();
        storage.initStorage(StorageFactorySwitch.get(true));
    }

    @After
    public void tearDownCommandStorageTest() throws Exception {
        storage.close();
        clearCurrentTenant();
    }

    private static CommandRecord newStorageRecord() {
        final Command command = Given.Command.createProject();
        final String commandType = TypeName.ofCommand(command)
                                           .value();

        final CommandRecord.Builder builder =
                CommandRecord.newBuilder()
                             .setCommandType(commandType)
                             .setCommandId(command.getId())
                             .setCommand(command)
                             .setTimestamp(getCurrentTime())
                             .setStatus(ProcessingStatus.newBuilder()
                                                        .setCode(RECEIVED));
        return builder.build();
    }

    /*
     * Storing and loading tests.
     ****************************/

    private Optional<CommandRecord> read(CommandId commandId) {
        final Optional<Entity> entity = storage.find(commandId);
        if (entity.isPresent()) {
            return Optional.of(entity.get()
                                     .getState());
        }
        return Optional.absent();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
    @Test
    public void store_and_read_command() {
        final Command command = Given.Command.createProject();
        final CommandId commandId = command.getId();

        storage.store(command);
        final CommandRecord record = read(commandId).get();

        checkRecord(record, command, RECEIVED);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
    @Test
    public void store_command_with_error() {
        final Command command = Given.Command.createProject();
        final CommandId commandId = command.getId();
        final Error error = newError();

        storage.store(command, error);
        final CommandRecord record = read(commandId).get();

        checkRecord(record, command, ERROR);
        assertEquals(error, record.getStatus()
                                  .getError());
    }

    @Test
    public void store_command_with_error_and_generate_ID_if_needed() {
        final TestActorRequestFactory factory = TestActorRequestFactory.newInstance(getClass());
        final Command command = factory.createCommand(createProjectMessage());
        final Error error = newError();

        storage.store(command, error);
        final List<CommandRecord> records = Lists.newArrayList(storage.iterator(ERROR));

        assertEquals(1, records.size());
        final String commandIdStr = idToString(records.get(0)
                                                      .getCommandId());
        assertFalse(commandIdStr.isEmpty());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
    @Test
    public void store_command_with_status() {
        final Command command = Given.Command.createProject();
        final CommandId commandId = command.getId();
        final CommandStatus status = SCHEDULED;

        storage.store(command, status);
        final CommandRecord record = read(commandId).get();

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

        final Iterator<CommandRecord> iterator = storage.iterator(status);
        final List<Command> actualCommands = newArrayList(toCommandIterator(iterator));
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

        final CommandRecord actual = read(id).get();
        assertEquals(OK, actual.getStatus()
                               .getCode());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
    @Test
    public void set_error_command_status() {
        givenNewRecord();
        final Error error = newError();

        storage.updateStatus(id, error);

        final CommandRecord actual = read(id).get();
        assertEquals(ERROR, actual.getStatus().getCode());
        assertEquals(error, actual.getStatus().getError());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
    @Test
    public void set_failure_command_status() {
        givenNewRecord();
        final Failure failure = newFailure();

        storage.updateStatus(id, failure);

        final CommandRecord actual = read(id).get();
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
        final Command command = Given.Command.createProject();
        final CommandStatus status = RECEIVED;

        final CommandRecord record = newRecordBuilder(command, status, null).build();

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
    public void throw_exception_if_try_to_store_cmd_to_closed_storage() throws Exception {
        storage.close();
        storage.store(Given.Command.createProject());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_store_cmd_with_error_to_closed_storage() throws
                                                                                   Exception {
        storage.close();
        storage.store(Given.Command.createProject(), newError());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_store_cmd_with_status_to_closed_storage() throws
                                                                                    Exception {
        storage.close();
        storage.store(Given.Command.createProject(), OK);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_load_commands_by_status_from_closed_storage() throws
                                                                                        Exception {
        storage.close();
        storage.iterator(OK);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_set_OK_status_using_closed_storage() throws Exception {
        storage.close();
        storage.setOkStatus(generateId());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_set_ERROR_status_using_closed_storage() throws Exception {
        storage.close();
        storage.updateStatus(generateId(), newError());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_try_to_set_FAILURE_status_using_closed_storage() throws
                                                                                    Exception {
        storage.close();
        storage.updateStatus(generateId(), newFailure());
    }

    @Test
    public void provide_null_accepting_record_retrieval_func() {
        assertNull(Storage.getRecordFunc()
                          .apply(null));
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
        final CommandRecord record = newStorageRecord();
        id = record.getCommandId();
        storage.store(record.getCommand());
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
        final Any packedFailureMessage = pack("newFailure");
        final FailureId id = Failures.generateId(Commands.generateId());
        return Failure.newBuilder()
                      .setId(id)
                      .setMessage(packedFailureMessage)
                      .setContext(FailureContext.newBuilder()
                                                .setStacktrace("failure stacktrace")
                                                .setTimestamp(getCurrentTime()))
                      .build();
    }
}
