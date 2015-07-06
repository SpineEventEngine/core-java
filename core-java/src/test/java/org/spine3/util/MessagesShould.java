/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.util;

import com.google.protobuf.Any;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.spine3.base.UserId;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Mikhail Melnik
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class MessagesShould {

    private UserId id;
    private Any any;

    @Before
    public void setUp() {
        id = UserIds.create("messages_test");
        any = Any.newBuilder()
                .setTypeUrl(id.getDescriptorForType().getFullName())
                .setValue(id.toByteString())
                .build();
    }

    @Test
    public void convert_id_to_Any() {
        Any result = Messages.toAny(id);

        assertThat(result, equalTo(any));
    }

    @Test
    @Ignore
    public void convert_from_Any_to_id() {
        UserId result = Messages.fromAny(any);

        assertThat(result, equalTo(id));
    }

    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_conver_null_id() {
        //noinspection ConstantConditions
        Messages.toAny(null);
    }

    @Test(expected = NullPointerException.class)
    public void fail_on_attempt_to_convert_from_null_Any() {
        //noinspection ConstantConditions
        Messages.fromAny(null);
    }

}
