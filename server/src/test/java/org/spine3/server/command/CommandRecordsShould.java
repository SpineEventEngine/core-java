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

import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandStatus;

import static org.spine3.base.CommandStatus.RECEIVED;
import static org.spine3.server.command.CommandRecords.newRecordBuilder;
import static org.spine3.server.command.CommandTestUtil.checkRecord;
import static org.spine3.server.command.Given.Command.createProject;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Alexander Yevsykov
 */
public class CommandRecordsShould {

    @Test
    public void have_utility_ctor() {
        assertHasPrivateParameterlessCtor(CommandRecords.class);
    }

    /*
     * Conversion tests.
     *******************/

    @Test
    public void convert_cmd_to_record() {
        final Command command = createProject();
        final CommandStatus status = RECEIVED;

        final CommandRecord record = newRecordBuilder(command,
                                                      status,
                                                      null).build();

        checkRecord(record, command, status);
    }
}
