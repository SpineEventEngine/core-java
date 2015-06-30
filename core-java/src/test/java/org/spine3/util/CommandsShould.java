/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
package org.spine3.util;

import org.junit.Test;
import org.spine3.base.CommandId;
import org.spine3.base.UserId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Mikhail Melnik
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class CommandsShould {

    @Test
    public void generate() {
        UserId userId = UserIds.create("commands-should-test");

        CommandId result = Commands.generateId(userId);

        //noinspection DuplicateStringLiteralInspection
        assertThat(result, allOf(
                hasProperty("actor", equalTo(userId)),
                hasProperty("timestamp")));
    }

    @Test(expected = NullPointerException.class)
    public void fail_on_null_parameter() {
        //noinspection ConstantConditions
        Commands.generateId(null);
    }


    @Test
    public void convert_field_name_to_method_name() {
        assertEquals("getUserId", Commands.toAccessorMethodName("user_id"));
        assertEquals("getId", Commands.toAccessorMethodName("id"));
        assertEquals("getAggregateRootId", Commands.toAccessorMethodName("aggregate_root_id"));
    }
}
