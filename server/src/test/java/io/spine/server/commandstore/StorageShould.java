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

package io.spine.server.commandstore;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.CommandStatus;
import io.spine.core.Commands;
import io.spine.core.Rejection;
import io.spine.core.RejectionContext;
import io.spine.core.RejectionId;
import io.spine.core.Rejections;
import io.spine.server.commandbus.CommandRecord;
import io.spine.server.commandbus.Given;
import io.spine.server.commandbus.ProcessingStatus;
import io.spine.server.storage.StorageFactorySwitch;
import io.spine.server.tenant.TenantAwareTest;
import io.spine.test.Tests;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.core.CommandStatus.ERROR;
import static io.spine.core.CommandStatus.OK;
import static io.spine.core.CommandStatus.RECEIVED;
import static io.spine.core.CommandStatus.REJECTED;
import static io.spine.core.CommandStatus.SCHEDULED;
import static io.spine.core.Commands.generateId;
import static io.spine.core.given.GivenTenantId.newUuid;
import static io.spine.protobuf.TypeConverter.toAny;
import static io.spine.server.BoundedContext.newName;
import static io.spine.server.commandbus.Given.CommandMessage.createProjectMessage;
import static io.spine.server.commandstore.CommandTestUtil.checkRecord;
import static io.spine.server.commandstore.Records.newRecordBuilder;
import static io.spine.server.commandstore.Records.toCommandIterator;
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
    private static final Rejection DEFAULT_REJECTION = Rejection.getDefaultInstance();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private CRepository repository;

    private CommandId id;

    @Before
    public void setUpCommandStorageTest() {
        setCurrentTenant(newUuid());
        repository = new CRepository();
        final StorageFactorySwitch storageSwitch =
                StorageFactorySwitch.newInstance(newName(getClass().getSimpleName()), true);
        repository.initStorage(storageSwitch.get());
    }

    @After
    public void tearDownCommandStorageTest() {
        repository.close();
        clearCurrentTenant();
    }

    private static CommandRecord newStorageRecord() {
        final Command command = Given.ACommand.createProject();
        final String commandType = CommandEnvelope.of(command)
                                                  .getTypeName()
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
        final Optional<CEntity> entity = repository.find(commandId);
        if (entity.isPresent()) {
            return Optional.of(entity.get()
                                     .getState());
        }
        return Optional.absent();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
    @Test
    public void store_and_read_command() {
        final Command command = Given.ACommand.createProject();
        final CommandId commandId = command.getId();

        repository.store(command);
        final CommandRecord record = read(commandId).get();

        checkRecord(record, command, RECEIVED);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
    @Test
    public void store_command_with_error() {
        final Command command = Given.ACommand.createProject();
        final CommandId commandId = command.getId();
        final Error error = newError();

        repository.store(command, error);
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

        repository.store(command, error);
        final List<CommandRecord> records = Lists.newArrayList(repository.iterator(ERROR));

        assertEquals(1, records.size());
        final String commandIdStr = Identifier.toString(records.get(0)
                                                               .getCommandId());
        assertFalse(commandIdStr.isEmpty());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
    @Test
    public void store_command_with_status() {
        final Command command = Given.ACommand.createProject();
        final CommandId commandId = command.getId();
        final CommandStatus status = SCHEDULED;

        repository.store(command, status);
        final CommandRecord record = read(commandId).get();

        checkRecord(record, command, status);
    }

    @Test
    public void load_commands_by_status() {
        final List<Command> commands = ImmutableList.of(Given.ACommand.createProject(),
                                                        Given.ACommand.addTask(),
                                                        Given.ACommand.startProject());
        final CommandStatus status = SCHEDULED;

        store(commands, status);
        // store an extra command with another status
        repository.store(Given.ACommand.createProject(), ERROR);

        final Iterator<CommandRecord> iterator = repository.iterator(status);
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

        repository.setOkStatus(id);

        final CommandRecord actual = read(id).get();
        assertEquals(OK, actual.getStatus()
                               .getCode());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
    @Test
    public void set_error_command_status() {
        givenNewRecord();
        final Error error = newError();

        repository.updateStatus(id, error);

        final CommandRecord actual = read(id).get();
        assertEquals(ERROR, actual.getStatus().getCode());
        assertEquals(error, actual.getStatus().getError());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
    @Test
    public void set_rejected_command_status() {
        givenNewRecord();
        final Rejection rejection = newRejection();

        repository.updateStatus(id, rejection);

        final CommandRecord actual = read(id).get();
        assertEquals(REJECTED, actual.getStatus()
                                     .getCode());
        assertEquals(rejection, actual.getStatus()
                                      .getRejection());
    }

    /*
     * Conversion tests.
     *******************/

    @Test
    public void convert_cmd_to_record() {
        final Command command = Given.ACommand.createProject();
        final CommandStatus status = RECEIVED;

        final CommandRecord record = newRecordBuilder(command, status, null).build();

        checkRecord(record, command, status);
    }

    /*
     * Check that exception is thrown if try to pass null to methods.
     **************************************************************/

    @Test
    public void throw_exception_if_try_to_store_null() {
        thrown.expect(NullPointerException.class);
        repository.store(Tests.<Command>nullRef());
    }

    @Test
    public void throw_exception_if_try_to_set_OK_status_by_null_ID() {
        thrown.expect(NullPointerException.class);
        repository.setOkStatus(Tests.nullRef());
    }

    @Test
    public void throw_exception_if_try_to_set_error_status_by_null_ID() {
        thrown.expect(NullPointerException.class);
        repository.updateStatus(Tests.<CommandId>nullRef(), defaultError);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_set_rejection_status_by_null_ID() {
        repository.updateStatus(Tests.nullRef(), DEFAULT_REJECTION);
    }

    /*
     * Check that exception is thrown if try to use closed storage.
     **************************************************************/

    @Test
    public void throw_exception_if_try_to_store_cmd_to_closed_storage() {
        repository.close();
        thrown.expect(IllegalStateException.class);
        repository.store(Given.ACommand.createProject());
    }

    @Test
    public void throw_exception_if_try_to_store_cmd_with_error_to_closed_storage() {
        repository.close();
        thrown.expect(IllegalStateException.class);
        repository.store(Given.ACommand.createProject(), newError());
    }

    @Test
    public void throw_exception_if_try_to_store_cmd_with_status_to_closed_storage() {
        repository.close();
        thrown.expect(IllegalStateException.class);
        repository.store(Given.ACommand.createProject(), OK);
    }

    @Test
    public void throw_exception_if_try_to_load_commands_by_status_from_closed_storage() {
        repository.close();
        thrown.expect(IllegalStateException.class);
        repository.iterator(OK);
    }

    @Test
    public void throw_exception_if_try_to_set_OK_status_using_closed_storage() {
        repository.close();
        thrown.expect(IllegalStateException.class);
        repository.setOkStatus(generateId());
    }

    @Test
    public void throw_exception_if_try_to_set_ERROR_status_using_closed_storage() {
        repository.close();
        thrown.expect(IllegalStateException.class);
        repository.updateStatus(generateId(), newError());
    }

    @Test
    public void throw_exception_if_try_to_set_REJECTED_status_using_closed_storage() {
        repository.close();
        thrown.expect(IllegalStateException.class);
        repository.updateStatus(generateId(), newRejection());
    }

    @Test
    public void provide_null_accepting_record_retrieval_func() {
        assertNull(CRepository.getRecordFunc()
                              .apply(null));
    }

    /*
     * Utils.
     **************************************************************/

    private void store(Iterable<Command> commands, CommandStatus status) {
        for (Command cmd : commands) {
            repository.store(cmd, status);
        }
    }

    private void givenNewRecord() {
        final CommandRecord record = newStorageRecord();
        id = record.getCommandId();
        repository.store(record.getCommand());
    }

    private static Error newError() {
        return Error.newBuilder()
                    .setType("error type 123")
                    .setCode(5)
                    .setMessage("error message 123")
                    .setStacktrace("stacktrace")
                    .build();
    }

    private static Rejection newRejection() {
        final Any packedMessage = toAny("newRejection");
        final RejectionId id = Rejections.generateId(Commands.generateId());
        return Rejection.newBuilder()
                        .setId(id)
                        .setMessage(packedMessage)
                        .setContext(RejectionContext.newBuilder()
                                                    .setStacktrace("rejection stacktrace")
                                                    .setTimestamp(getCurrentTime()))
                        .build();
    }
}
