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

package org.spine3.server.entity;

import com.google.common.base.Optional;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.server.entity.idfunc.GetTargetIdFromCommand;
import org.spine3.test.entity.command.CreateProject;
import org.spine3.testdata.Sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 */
public class GetTargetIdFromCommandShould {

    @Test
    public void return_empty_Optional_if_fail_to_get_ID_from_command_message_without_ID_field() {
        final Optional id = GetTargetIdFromCommand.asOptional(StringValue.getDefaultInstance());

        assertFalse(id.isPresent());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We call isPresent() in assertion.
    @Test
    public void get_ID_from_command_message() {
        final CreateProject msg = Sample.messageOfType(CreateProject.class);

        final Optional id = GetTargetIdFromCommand.asOptional(msg);

        assertTrue(id.isPresent());
        assertEquals(msg.getProjectId(), id.get());
    }
}
