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

import com.google.protobuf.Any;
import org.junit.Before;
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
