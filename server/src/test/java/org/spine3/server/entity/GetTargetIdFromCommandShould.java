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

package org.spine3.server.entity;

import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.test.project.ProjectId;
import org.spine3.test.project.command.CreateProject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.spine3.testdata.TestCommands.createProjectMsg;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class GetTargetIdFromCommandShould {

    @Test
    public void return_null_if_fail_to_get_ID_from_command_message_without_ID_field() {
        final Object id = GetTargetIdFromCommand.asNullableObject(StringValue.getDefaultInstance());

        assertNull(id);
    }

    @Test
    public void get_ID_from_command_message() {
        final CreateProject msg = createProjectMsg();

        final ProjectId id = GetTargetIdFromCommand.asNullableObject(msg);

        assertEquals(msg.getProjectId(), id);
    }
}
