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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ConstantConditions")
@DisplayName("Storage should")
class StorageTest extends TenantAwareTest {

    private static final Error defaultError = Error.getDefaultInstance();
    private static final Rejection DEFAULT_REJECTION = Rejection.getDefaultInstance();

    private CRepository repository;

    private CommandId id;

    @BeforeEach
    void setUpCommandStorageTest() {
        setCurrentTenant(newUuid());
        repository = new CRepository();
        final StorageFactorySwitch storageSwitch =
                StorageFactorySwitch.newInstance(newName(getClass().getSimpleName()), true);
        repository.initStorage(storageSwitch.get());
    }

    @AfterEach
    void tearDownCommandStorageTest() {
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
    @DisplayName("store and read command")
    void storeAndReadCommand() {
        final Command command = Given.ACommand.createProject();
        final CommandId commandId = command.getId();

        repository.store(command);
        final CommandRecord record = read(commandId).get();

        checkRecord(record, command, RECEIVED);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
    @Test
    @DisplayName("store command with error")
    void storeCommandWithError() {
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
    @DisplayName("store command with error and generate ID if needed")
    void storeCmdWithErrorAndGenerateId() {
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
    @DisplayName("store command with status")
    void storeCommandWithStatus() {
        final Command command = Given.ACommand.createProject();
        final CommandId commandId = command.getId();
        final CommandStatus status = SCHEDULED;

        repository.store(command, status);
        final CommandRecord record = read(commandId).get();

        checkRecord(record, command, status);
    }

    @Test
    @DisplayName("load commands by status")
    void loadCommandsByStatus() {
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
    @DisplayName("set ok command status")
    void setOkCommandStatus() {
        givenNewRecord();

        repository.setOkStatus(id);

        final CommandRecord actual = read(id).get();
        assertEquals(OK, actual.getStatus()
                               .getCode());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
    @Test
    @DisplayName("set error command status")
    void setErrorCommandStatus() {
        givenNewRecord();
        final Error error = newError();

        repository.updateStatus(id, error);

        final CommandRecord actual = read(id).get();
        assertEquals(ERROR, actual.getStatus().getCode());
        assertEquals(error, actual.getStatus().getError());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
    @Test
    @DisplayName("set rejected command status")
    void setRejectedCommandStatus() {
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

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("convert command to record")
    void convertCommandToRecord() {
        final Command command = Given.ACommand.createProject();
        final CommandStatus status = RECEIVED;

        final CommandRecord record = newRecordBuilder(command, status, null).build();

        checkRecord(record, command, status);
    }

    /*
     * Check that exception is thrown if try to pass null to methods.
     **************************************************************/

    @Test
    @DisplayName("throw NPE if try to store null")
    void throwWhenStoringNull() {
        assertThrows(NullPointerException.class, () -> repository.store(Tests.<Command>nullRef()));
    }

    @Test
    @DisplayName("throw NPE if try to set OK status by null ID")
    void notSetOkForNullId() {
        assertThrows(NullPointerException.class, () -> repository.setOkStatus(Tests.nullRef()));
    }

    @Test
    @DisplayName("throw NPE if try to set error status by null ID")
    void notSetErrorForNullId() {
        assertThrows(NullPointerException.class,
                     () -> repository.updateStatus(Tests.nullRef(), defaultError));
    }

    @Test
    @DisplayName("throw NPE if try to set rejection status by null ID")
    void notSetRejectionForNullId() {
        assertThrows(NullPointerException.class,
                     () -> repository.updateStatus(Tests.nullRef(), DEFAULT_REJECTION));
    }

    /*
     * Check that exception is thrown if try to use closed storage.
     **************************************************************/

    @Test
    @DisplayName("throw ISE if try to store command to closed storage")
    void notStoreCmdWhenClosed() {
        repository.close();
        assertThrows(IllegalStateException.class,
                     () -> repository.store(Given.ACommand.createProject()));
    }

    @Test
    @DisplayName("throw ISE if try to store command with error to closed storage")
    void notStoreCmdWithErrorWhenClosed() {
        repository.close();
        assertThrows(IllegalStateException.class,
                     () -> repository.store(Given.ACommand.createProject(), newError()));
    }

    @Test
    @DisplayName("throw ISE if try to store command with status to closed storage")
    void notStoreCmdWithStatusWhenClosed() {
        repository.close();
        assertThrows(IllegalStateException.class,
                     () -> repository.store(Given.ACommand.createProject(), OK));
    }

    @Test
    @DisplayName("throw ISE if try to load commands by status from closed storage")
    void notLoadByStatusWhenClosed() {
        repository.close();
        assertThrows(IllegalStateException.class, () -> repository.iterator(OK));
    }

    @Test
    @DisplayName("throw ISE if try to set OK status using closed storage")
    void notSetOkStatusWhenClosed() {
        repository.close();
        assertThrows(IllegalStateException.class, () -> repository.setOkStatus(generateId()));
    }

    @Test
    @DisplayName("throw ISE if try to set ERROR status using closed storage")
    void notSetErrorStatusWhenClosed() {
        repository.close();
        assertThrows(IllegalStateException.class,
                     () -> repository.updateStatus(generateId(), newError()));
    }

    @Test
    @DisplayName("throw ISE if try to set REJECTED status using closed storage")
    void notSetRejectionStatusWhenClosed() {
        repository.close();
        assertThrows(IllegalStateException.class,
                     () -> repository.updateStatus(generateId(), newRejection()));
    }

    @Test
    @DisplayName("provide null accepting record retrieval function")
    void provideNullAcceptingRecordFunc() {
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
