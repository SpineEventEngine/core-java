/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
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
