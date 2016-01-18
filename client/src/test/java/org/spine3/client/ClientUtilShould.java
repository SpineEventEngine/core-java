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

package org.spine3.client;

import org.junit.Test;
import org.spine3.base.CommandId;
import org.spine3.base.UserId;
import org.spine3.util.Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.spine3.client.ClientUtil.generateId;
import static org.spine3.client.ClientUtil.newUserId;

@SuppressWarnings("InstanceMethodNamingConvention")
public class ClientUtilShould {

    @Test
    public void have_private_constructor() throws Exception {
        Tests.callPrivateUtilityConstructor(ClientUtil.class);
    }

    @Test
    public void generate_command_Ids() {
        final CommandId result = generateId();

        assertFalse(result.getUuid().isEmpty());
    }

    @Test
    public void create_UserId_by_string() {

        final String testIdString = "12345";
        final UserId userId = newUserId(testIdString);

        final UserId expected = UserId.newBuilder().setValue(testIdString).build();

        assertEquals(expected, userId);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_UseId_value() {
        newUserId(null);
    }

}