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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.CommandStatus;
import io.spine.core.Rejection;
import io.spine.server.commandbus.CommandRecord;
import io.spine.server.commandbus.Given;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.storage.StorageFactorySwitch;
import io.spine.testing.Tests;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.tenant.TenantAwareTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.core.BoundedContextNames.newName;
import static io.spine.core.CommandStatus.ERROR;
import static io.spine.core.CommandStatus.OK;
import static io.spine.core.CommandStatus.RECEIVED;
import static io.spine.core.CommandStatus.REJECTED;
import static io.spine.core.CommandStatus.SCHEDULED;
import static io.spine.core.Commands.generateId;
import static io.spine.server.commandbus.Given.CommandMessage.createProjectMessage;
import static io.spine.server.commandstore.CommandTestUtil.checkRecord;
import static io.spine.server.commandstore.Records.newRecordBuilder;
import static io.spine.server.commandstore.Records.toCommandIterator;
import static io.spine.server.commandstore.given.StorageTestEnv.newError;
import static io.spine.server.commandstore.given.StorageTestEnv.newRejection;
import static io.spine.server.commandstore.given.StorageTestEnv.newStorageRecord;
import static io.spine.testing.core.given.GivenTenantId.newUuid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"ConstantConditions",
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("Command Storage should")
class StorageTest extends TenantAwareTest {

    private static final Error defaultError = Error.getDefaultInstance();
    private static final Rejection DEFAULT_REJECTION = Rejection.getDefaultInstance();

    private CRepository repository;

    private CommandId id;

    private Optional<CommandRecord> readRecord(CommandId commandId) {
        Optional<CEntity> entity = repository.find(commandId);
        return entity.map(AbstractEntity::getState);
    }

    @BeforeEach
    void setUpCommandStorageTest() {
        setCurrentTenant(newUuid());
        repository = new CRepository();
        StorageFactorySwitch storageSwitch =
                StorageFactorySwitch.newInstance(newName(getClass().getSimpleName()), true);
        repository.initStorage(storageSwitch.get());
    }

    @AfterEach
    void tearDownCommandStorageTest() {
        repository.close();
        clearCurrentTenant();
    }

    @Nested
    @DisplayName("not accept null")
    class NotAcceptNull {

        @Test
        @DisplayName("command for storing")
        void command() {
            assertThrows(NullPointerException.class,
                         () -> repository.store(Tests.<Command>nullRef()));
        }

        @Test
        @DisplayName("command ID for setting `OK` status")
        void commandIdForOkStatus() {
            assertThrows(NullPointerException.class, () -> repository.setOkStatus(Tests.nullRef()));
        }

        @Test
        @DisplayName("command ID for setting `ERROR` status")
        void commandIdForErrorStatus() {
            assertThrows(NullPointerException.class,
                         () -> repository.updateStatus(Tests.nullRef(), defaultError));
        }

        @Test
        @DisplayName("command ID for setting `REJECTED` status")
        void commandIdForRejectionStatus() {
            assertThrows(NullPointerException.class,
                         () -> repository.updateStatus(Tests.nullRef(), DEFAULT_REJECTION));
        }
    }

    @Nested
    @DisplayName("store and read")
    class StoreAndRead {

        @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
        @Test
        @DisplayName("command")
        void command() {
            Command command = Given.ACommand.createProject();
            CommandId commandId = command.getId();

            repository.store(command);
            CommandRecord record = readRecord(commandId).get();

            checkRecord(record, command, RECEIVED);
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
        @Test
        @DisplayName("command with error")
        void commandWithError() {
            Command command = Given.ACommand.createProject();
            CommandId commandId = command.getId();
            Error error = newError();

            repository.store(command, error);
            CommandRecord record = readRecord(commandId).get();

            checkRecord(record, command, ERROR);
            assertEquals(error, record.getStatus()
                                      .getError());
        }

        @Test
        @DisplayName("command with error having no ID")
        void commandWithErrorWithoutId() {
            TestActorRequestFactory factory = TestActorRequestFactory.newInstance(getClass());
            Command command = factory.createCommand(createProjectMessage());
            Error error = newError();

            repository.store(command, error);
            List<CommandRecord> records = Lists.newArrayList(repository.iterator(ERROR));

            assertEquals(1, records.size());
            String commandIdStr = Identifier.toString(records.get(0)
                                                             .getCommandId());
            assertFalse(commandIdStr.isEmpty());
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we store.
        @Test
        @DisplayName("command with status")
        void commandWithStatus() {
            Command command = Given.ACommand.createProject();
            CommandId commandId = command.getId();
            CommandStatus status = SCHEDULED;

            repository.store(command, status);
            CommandRecord record = readRecord(commandId).get();

            checkRecord(record, command, status);
        }

        @Test
        @DisplayName("multiple commands with status")
        void multipleCommandsWithStatus() {
            List<Command> commands = ImmutableList.of(Given.ACommand.createProject(),
                                                      Given.ACommand.addTask(),
                                                      Given.ACommand.startProject());
            CommandStatus status = SCHEDULED;

            store(commands, status);
            // store an extra command with another status
            repository.store(Given.ACommand.createProject(), ERROR);

            Iterator<CommandRecord> iterator = repository.iterator(status);
            List<Command> actualCommands = newArrayList(toCommandIterator(iterator));
            assertEquals(commands.size(), actualCommands.size());
            for (Command cmd : actualCommands) {
                assertTrue(commands.contains(cmd));
            }
        }

        private void store(Iterable<Command> commands, CommandStatus status) {
            for (Command cmd : commands) {
                repository.store(cmd, status);
            }
        }
    }

    @Nested
    @DisplayName("set command status to")
    class SetCommandStatusTo {

        @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
        @Test
        @DisplayName("`OK`")
        void ok() {
            storeNewRecord();

            repository.setOkStatus(id);

            CommandRecord actual = readRecord(id).get();
            assertEquals(OK, actual.getStatus()
                                   .getCode());
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
        @Test
        @DisplayName("`ERROR`")
        void error() {
            storeNewRecord();
            Error error = newError();

            repository.updateStatus(id, error);

            CommandRecord actual = readRecord(id).get();
            assertEquals(ERROR, actual.getStatus()
                                      .getCode());
            assertEquals(error, actual.getStatus()
                                      .getError());
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we update status.
        @Test
        @DisplayName("`REJECTED`")
        void rejection() {
            storeNewRecord();
            Rejection rejection = newRejection();

            repository.updateStatus(id, rejection);

            CommandRecord actual = readRecord(id).get();
            assertEquals(REJECTED, actual.getStatus()
                                         .getCode());
            assertEquals(rejection, actual.getStatus()
                                          .getRejection());
        }

        private void storeNewRecord() {
            CommandRecord record = newStorageRecord();
            id = record.getCommandId();
            repository.store(record.getCommand());
        }
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("convert command to record")
    void convertCommandToRecord() {
        Command command = Given.ACommand.createProject();
        CommandStatus status = RECEIVED;

        CommandRecord record = newRecordBuilder(command, status, null).build();

        checkRecord(record, command, status);
    }

    @Nested
    @DisplayName("if closed, throw ISE on")
    class ThrowIseIfClosed {

        @Test
        @DisplayName("storing command")
        void whenStoringCmd() {
            repository.close();
            assertThrows(IllegalStateException.class,
                         () -> repository.store(Given.ACommand.createProject()));
        }

        @Test
        @DisplayName("storing command with error")
        void whenStoringCmdWithError() {
            repository.close();
            assertThrows(IllegalStateException.class,
                         () -> repository.store(Given.ACommand.createProject(), newError()));
        }

        @Test
        @DisplayName("storing command with status")
        void whenStoringCmdWithStatus() {
            repository.close();
            assertThrows(IllegalStateException.class,
                         () -> repository.store(Given.ACommand.createProject(), OK));
        }

        @Test
        @DisplayName("loading commands by status")
        void whenLoadingCmd() {
            repository.close();
            assertThrows(IllegalStateException.class, () -> repository.iterator(OK));
        }

        @Test
        @DisplayName("setting `OK` status")
        void whenSettingOk() {
            repository.close();
            assertThrows(IllegalStateException.class, () -> repository.setOkStatus(generateId()));
        }

        @Test
        @DisplayName("setting `ERROR` status")
        void whenSettingError() {
            repository.close();
            assertThrows(IllegalStateException.class,
                         () -> repository.updateStatus(generateId(), newError()));
        }

        @Test
        @DisplayName("setting `REJECTED` status")
        void whenSettingRejected() {
            repository.close();
            assertThrows(IllegalStateException.class,
                         () -> repository.updateStatus(generateId(), newRejection()));
        }
    }

    @Test
    @DisplayName("provide null accepting record retrieval function")
    void provideNullAcceptingRecordFunc() {
        assertNull(CRepository.getRecordFunc()
                              .apply(null));
    }
}
