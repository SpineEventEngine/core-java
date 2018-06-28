/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.stand;

import com.google.protobuf.Any;
import io.spine.string.Stringifier;
import io.spine.string.Stringifiers;
import io.spine.test.projection.ProjectId;
import io.spine.testdata.Sample;
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.stand.AggregateStateId.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("AggregateStateIdStringifier should")
class AggregateStateIdStringifierTest {

    private static final TypeUrl ANY_TYPE_URL = TypeUrl.of(Any.class);

    @Test
    @DisplayName("accept string IDs")
    void acceptStringIds() {
        final Stringifier<AggregateStateId> stringifier = stringifier();
        final AggregateStateId id = newStringId();

        final String stringAggregateId = stringifier.convert(id);
        assertNotNull(stringAggregateId);
        assertThat(stringAggregateId, CoreMatchers.containsString(id.getAggregateId()
                                                                    .toString()));
    }

    @Test
    @DisplayName("accept int IDs")
    void acceptIntIds() {
        final Stringifier<AggregateStateId> stringifier = stringifier();
        final AggregateStateId id = newIntId();

        final String stringAggregateId = stringifier.convert(id);
        assertNotNull(stringAggregateId);
        assertThat(stringAggregateId, CoreMatchers.containsString(id.getAggregateId()
                                                                    .toString()));
    }

    @Test
    @DisplayName("accept long IDs")
    void acceptLongIds() {
        final Stringifier<AggregateStateId> stringifier = stringifier();
        final AggregateStateId id = newLongId();

        final String stringAggregateId = stringifier.convert(id);
        assertNotNull(stringAggregateId);
        assertThat(stringAggregateId, CoreMatchers.containsString(id.getAggregateId()
                                                                    .toString()));
    }

    @Test
    @DisplayName("accept message IDs of registered types")
    void acceptMessageIdsOfRegisteredTypes() {
        final Stringifier<AggregateStateId> stringifier = stringifier();
        final AggregateStateId id = newMessageId();

        final String stringAggregateId = stringifier.convert(id);
        assertNotNull(stringAggregateId);
        assertThat(stringAggregateId,
                   CoreMatchers.containsString(Stringifiers.toString(id.getAggregateId())));
    }

    @Test
    @DisplayName("unpack int IDs")
    void unpackIntIds() {
        final int intId = 42;
        final String stringId = ANY_TYPE_URL.value() + "-Integer-" + String.valueOf(intId);
        final Stringifier<AggregateStateId> stringifier = stringifier();

        final AggregateStateId id = stringifier.reverse()
                                               .convert(stringId);

        assertNotNull(id);
        assertEquals(ANY_TYPE_URL, id.getStateType());
        assertEquals(intId, id.getAggregateId());
    }

    @Test
    @DisplayName("unpack long IDs")
    void unpackLongIds() {
        final long longId = 31415;
        final String stringId = ANY_TYPE_URL.value() + "-Long-" + String.valueOf(longId);
        final Stringifier<AggregateStateId> stringifier = stringifier();

        final AggregateStateId id = stringifier.reverse()
                                               .convert(stringId);

        assertNotNull(id);
        assertEquals(ANY_TYPE_URL, id.getStateType());
        assertEquals(longId, id.getAggregateId());
    }

    @Test
    @DisplayName("unpack string IDs")
    void unpackStringIds() {
        final String stringIdValue = "abcde";
        final String stringId = ANY_TYPE_URL.value() + "-String-" + stringIdValue;
        final Stringifier<AggregateStateId> stringifier = stringifier();

        final AggregateStateId id = stringifier.reverse()
                                               .convert(stringId);

        assertNotNull(id);
        assertEquals(ANY_TYPE_URL, id.getStateType());
        assertEquals(stringIdValue, id.getAggregateId());
    }

    @Test
    @DisplayName("unpack registered message IDs")
    void unpackRegisteredMessageIds() {
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

    @Test
    @DisplayName("fail to convert invalid string")
    void failToConvertInvalidString() {
        final String invalidId = "I'm invalid!";
        assertThrows(IllegalArgumentException.class,
                     () -> stringifier().reverse()
                                        .convert(invalidId));
    }

    @Test
    @DisplayName("fail to convert string with no ID type")
    void failToConvertStringWithNoIDType() {
        final String invalidId = "google.protobuf/google.protobuf.Any-42";
        assertThrows(IllegalArgumentException.class,
                     () -> stringifier().reverse()
                                        .convert(invalidId));
    }

    @Test
    @DisplayName("fail to convert string with no state type URL")
    void failToConvertStringWithNoStateTypeURL() {
        final String invalidId = "-INT-42";
        assertThrows(IllegalArgumentException.class,
                     () -> stringifier().reverse()
                                        .convert(invalidId));
    }

    @Test
    @DisplayName("convert objects back and forth")
    void convertObjectsBackAndForth() {
        final Stringifier<AggregateStateId> stringifier = stringifier();
        final AggregateStateId id = newMessageId();

        final String stringAggregateId = stringifier.convert(id);
        assertNotNull(stringAggregateId);

        final AggregateStateId restored = stringifier.reverse()
                                                     .convert(stringAggregateId);
        assertEquals(id, restored);
    }

    private static AggregateStateId newStringId() {
        return of("some-aggregate-ID", TypeUrl.of(Any.class));
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
        return AggregateStateIdStringifier.getInstance();
    }
}
