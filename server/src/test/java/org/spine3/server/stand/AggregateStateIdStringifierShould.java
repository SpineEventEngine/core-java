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

package org.spine3.server.stand;

import com.google.protobuf.Any;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.spine3.base.Stringifier;
import org.spine3.base.Stringifiers;
import org.spine3.test.projection.ProjectId;
import org.spine3.testdata.Sample;
import org.spine3.type.TypeName;
import org.spine3.type.TypeUrl;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.spine3.server.stand.AggregateStateId.of;

/**
 * @author Dmytro Dashenkov
 */
public class AggregateStateIdStringifierShould {

    private static final TypeUrl ANY_TYPE_URL = TypeUrl.of(Any.class);

    @Test
    public void accept_string_ids() {
        final Stringifier<AggregateStateId> stringifier = stringifier();
        final AggregateStateId id = newStringId();

        final String stringAggregateId = stringifier.convert(id);
        assertNotNull(stringAggregateId);
        assertThat(stringAggregateId, CoreMatchers.containsString(id.getAggregateId()
                                                                    .toString()));
    }

    @Test
    public void accept_int_ids() {
        final Stringifier<AggregateStateId> stringifier = stringifier();
        final AggregateStateId id = newIntId();

        final String stringAggregateId = stringifier.convert(id);
        assertNotNull(stringAggregateId);
        assertThat(stringAggregateId, CoreMatchers.containsString(id.getAggregateId()
                                                                    .toString()));
    }

    @Test
    public void accept_long_ids() {
        final Stringifier<AggregateStateId> stringifier = stringifier();
        final AggregateStateId id = newLongId();

        final String stringAggregateId = stringifier.convert(id);
        assertNotNull(stringAggregateId);
        assertThat(stringAggregateId, CoreMatchers.containsString(id.getAggregateId()
                                                                    .toString()));
    }

    @Test
    public void accept_message_ids_of_registered_types() {
        final Stringifier<AggregateStateId> stringifier = stringifier();
        final AggregateStateId id = newMessageId();

        final String stringAggregateId = stringifier.convert(id);
        assertNotNull(stringAggregateId);
        assertThat(stringAggregateId,
                   CoreMatchers.containsString(Stringifiers.toString(id.getAggregateId())));
    }

    @Test
    public void unpack_int_ids() {
        final int intId = 42;
        final String stringId = ANY_TYPE_URL.value() + "-INT-" + String.valueOf(intId);
        final Stringifier<AggregateStateId> stringifier = stringifier();

        final AggregateStateId id = stringifier.reverse()
                                               .convert(stringId);

        assertNotNull(id);
        assertEquals(ANY_TYPE_URL, id.getStateType());
        assertEquals(intId, id.getAggregateId());
    }

    @Test
    public void unpack_long_ids() {
        final long longId = 31415;
        final String stringId = ANY_TYPE_URL.value() + "-LONG-" + String.valueOf(longId);
        final Stringifier<AggregateStateId> stringifier = stringifier();

        final AggregateStateId id = stringifier.reverse()
                                               .convert(stringId);

        assertNotNull(id);
        assertEquals(ANY_TYPE_URL, id.getStateType());
        assertEquals(longId, id.getAggregateId());
    }

    @Test
    public void unpack_string_ids() {
        final String stringIdValue = "abcde";
        final String stringId = ANY_TYPE_URL.value() + "-STRING-" + stringIdValue;
        final Stringifier<AggregateStateId> stringifier = stringifier();

        final AggregateStateId id = stringifier.reverse()
                                               .convert(stringId);

        assertNotNull(id);
        assertEquals(ANY_TYPE_URL, id.getStateType());
        assertEquals(stringIdValue, id.getAggregateId());
    }

    @Test
    public void unpack_registered_message_ids() {
        final ProjectId messageId = Sample.messageOfType(ProjectId.class);
        final String stringMessageId = Stringifiers.toString(messageId);
        final String stringId = ANY_TYPE_URL.value() + '-' + TypeName.of(ProjectId.class)
                + '-' + stringMessageId;
        final Stringifier<AggregateStateId> stringifier = stringifier();

        final AggregateStateId id = stringifier.reverse()
                                               .convert(stringId);

        assertNotNull(id);
        assertEquals(ANY_TYPE_URL, id.getStateType());
        assertEquals(messageId, id.getAggregateId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_convert_invalid_string() {
        final String invalidId = "I'm invalid!";
        stringifier()
                        .reverse()
                        .convert(invalidId);
    }

    @Test
    public void convert_objects_back_and_forth() {
        final Stringifier<AggregateStateId> stringifier = stringifier();
        final AggregateStateId id = newMessageId();

        final String stringAggregateId = stringifier.convert(id);
        assertNotNull(stringAggregateId);

        final AggregateStateId restored = stringifier.reverse().convert(stringAggregateId);
        assertEquals(id, restored);
    }

    private static AggregateStateId newStringId() {
        return of("some-aggregate-id", TypeUrl.of(Any.class));
    }

    private static AggregateStateId newIntId() {
        return of(42, TypeUrl.of(Any.class));
    }

    private static AggregateStateId newLongId() {
        return of(42L, TypeUrl.of(Any.class));
    }

    private static AggregateStateId newMessageId() {
        return of(Sample.messageOfType(ProjectId.class), TypeUrl.of(Any.class));
    }

    private static Stringifier<AggregateStateId> stringifier() {
        return new AggregateStateIdStringifier();
    }

    private static class ProjectIdStringifier extends Stringifier<ProjectId> {

        private static final Map<String, ProjectId> convertedInstances = new HashMap<>();

        @Override
        protected String toString(ProjectId obj) {
            final String stringValue = obj.toString();
            convertedInstances.put(stringValue, obj);
            return stringValue;
        }

        @Override
        protected ProjectId fromString(String s) {
            return convertedInstances.get(s);
        }
    }
}
