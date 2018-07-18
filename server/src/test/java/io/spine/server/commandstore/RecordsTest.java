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

import io.spine.core.Command;
import io.spine.core.CommandStatus;
import io.spine.server.commandbus.CommandRecord;
import io.spine.server.commandbus.Given;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.commandstore.CommandTestUtil.checkRecord;
import static io.spine.server.commandstore.Records.newRecordBuilder;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Alexander Yevsykov
 */
@DisplayName("Records utility should")
class RecordsTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(Records.class);
    }

    /*
     * Conversion tests.
     *******************/

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("convert command to record")
    void convertCmdToRecord() {
        final Command command = Given.ACommand.createProject();
        final CommandStatus status = CommandStatus.RECEIVED;

        final CommandRecord record = newRecordBuilder(command,
                                                      status,
                                                      null).build();

        checkRecord(record, command, status);
    }
}
