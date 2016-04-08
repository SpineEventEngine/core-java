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

package org.spine3.server.validate;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Int64Value;
import org.junit.Test;
import org.spine3.base.FieldPath;
import org.spine3.protobuf.Messages;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class LongFieldValidatorShould {

    private static final Long VALUE = 2L;
    private static final Long NEGATIVE_VALUE = -2L;

    private final LongFieldValidator validator =
            new LongFieldValidator(Any.getDescriptor().getFields().get(0), ImmutableList.of(VALUE), FieldPath.getDefaultInstance());

    @Test
    public void convert_string_to_number() {
        assertEquals(VALUE, validator.toNumber(VALUE.toString()));
    }

    @Test
    public void return_absolute_number_value() {
        assertEquals(VALUE, validator.getAbs(NEGATIVE_VALUE));
    }

    @Test
    public void wrap_to_any() {
        final Any any = validator.wrap(VALUE);
        final Int64Value msg = Messages.fromAny(any);
        assertEquals(VALUE, (Long) msg.getValue());
    }
}
