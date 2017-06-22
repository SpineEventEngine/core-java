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

import io.spine.base.Command;
import io.spine.base.CommandStatus;
import io.spine.server.commandbus.CommandRecord;
import io.spine.server.commandbus.Given;
import org.junit.Test;

import static io.spine.server.commandstore.CommandTestUtil.checkRecord;
import static io.spine.server.commandstore.Records.newRecordBuilder;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Alexander Yevsykov
 */
public class RecordsShould {

    @Test
    public void have_utility_ctor() {
        assertHasPrivateParameterlessCtor(Records.class);
    }

    /*
     * Conversion tests.
     *******************/

    @Test
    public void convert_cmd_to_record() {
        final Command command = Given.Command.createProject();
        final CommandStatus status = CommandStatus.RECEIVED;

        final CommandRecord record = newRecordBuilder(command,
                                                      status,
                                                      null).build();

        checkRecord(record, command, status);
    }
}
