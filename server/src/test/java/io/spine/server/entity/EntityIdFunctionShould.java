/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.entity;

import com.google.common.base.Function;
import com.google.protobuf.StringValue;
import io.spine.client.EntityId;
import org.junit.Test;

import static io.spine.protobuf.TypeConverter.toAny;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Alexander Yevsyukov
 */
public class EntityIdFunctionShould {

    @Test(expected = IllegalStateException.class)
    public void do_not_accept_wrong_id_type() {
        final Function<EntityId, StringValue> func =
                new RecordBasedRepository.EntityIdFunction<>(StringValue.class);

        final EntityId wrongType = EntityId.newBuilder()
                                           .setId(toAny(100L))
                                           .build();
        func.apply(wrongType);
    }

    @Test
    public void accept_proper_id_type() {
        final Function<EntityId, StringValue> func =
                new RecordBasedRepository.EntityIdFunction<>(StringValue.class);

        final String value = "abcd";
        final EntityId type = EntityId.newBuilder()
                                      .setId(toAny(value))
                                      .build();
        final StringValue result = func.apply(type);
        assertNotNull(result);
        assertEquals(value, result.getValue());
    }
}
