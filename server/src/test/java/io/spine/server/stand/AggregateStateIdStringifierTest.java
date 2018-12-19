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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.server.stand.given.AggregateStateIdStringifierTestEnv.newIntId;
import static io.spine.server.stand.given.AggregateStateIdStringifierTestEnv.newLongId;
import static io.spine.server.stand.given.AggregateStateIdStringifierTestEnv.newMessageId;
import static io.spine.server.stand.given.AggregateStateIdStringifierTestEnv.newStringId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names */})
@DisplayName("AggregateStateIdStringifier should")
class AggregateStateIdStringifierTest {

    private static final TypeUrl ANY_TYPE_URL = TypeUrl.of(Any.class);

    @Nested
    @DisplayName("accept IDs of type")
    class AcceptIds {

        @Test
        @DisplayName("String")
        void ofStringType() {
            Stringifier<AggregateStateId> stringifier = stringifier();
            AggregateStateId id = newStringId();

            String stringAggregateId = stringifier.convert(id);
            assertNotNull(stringAggregateId);
            assertThat(stringAggregateId, CoreMatchers.containsString(id.getAggregateId()
                                                                        .toString()));
        }

        @Test
        @DisplayName("int")
        void ofIntType() {
            Stringifier<AggregateStateId> stringifier = stringifier();
            AggregateStateId id = newIntId();

            String stringAggregateId = stringifier.convert(id);
            assertNotNull(stringAggregateId);
            assertThat(stringAggregateId, CoreMatchers.containsString(id.getAggregateId()
                                                                        .toString()));
        }

        @Test
        @DisplayName("long")
        void ofLongType() {
            Stringifier<AggregateStateId> stringifier = stringifier();
            AggregateStateId id = newLongId();

            String stringAggregateId = stringifier.convert(id);
            assertNotNull(stringAggregateId);
            assertThat(stringAggregateId, CoreMatchers.containsString(id.getAggregateId()
                                                                        .toString()));
        }

        @Test
        @DisplayName("Message, if type is registered")
        void ofMessageType() {
            Stringifier<AggregateStateId> stringifier = stringifier();
            AggregateStateId id = newMessageId();

            String stringAggregateId = stringifier.convert(id);
            assertNotNull(stringAggregateId);
            assertThat(stringAggregateId,
                       CoreMatchers.containsString(Stringifiers.toString(id.getAggregateId())));
        }
    }

    @Nested
    @DisplayName("unpack IDs of type")
    class UnpackIds {

        @Test
        @DisplayName("String")
        void ofStringType() {
            String stringIdValue = "abcde";
            String stringId = ANY_TYPE_URL.value() + "-String-" + stringIdValue;
            Stringifier<AggregateStateId> stringifier = stringifier();

            AggregateStateId id = stringifier.reverse()
                                             .convert(stringId);

            assertNotNull(id);
            assertEquals(ANY_TYPE_URL, id.getStateType());
            assertEquals(stringIdValue, id.getAggregateId());
        }

        @Test
        @DisplayName("int")
        void ofIntType() {
            int intId = 42;
            String stringId = ANY_TYPE_URL.value() + "-Integer-" + String.valueOf(intId);
            Stringifier<AggregateStateId> stringifier = stringifier();

            AggregateStateId id = stringifier.reverse()
                                             .convert(stringId);

            assertNotNull(id);
            assertEquals(ANY_TYPE_URL, id.getStateType());
            assertEquals(intId, id.getAggregateId());
        }

        @Test
        @DisplayName("long")
        void ofLongType() {
            long longId = 31415;
            String stringId = ANY_TYPE_URL.value() + "-Long-" + String.valueOf(longId);
            Stringifier<AggregateStateId> stringifier = stringifier();

            AggregateStateId id = stringifier.reverse()
                                             .convert(stringId);

            assertNotNull(id);
            assertEquals(ANY_TYPE_URL, id.getStateType());
            assertEquals(longId, id.getAggregateId());
        }

        @Test
        @DisplayName("Message, if type is registered")
        void ofMessageType() {
            ProjectId messageId = Sample.messageOfType(ProjectId.class);
            String stringMessageId = Stringifiers.toString(messageId);
            String stringId = ANY_TYPE_URL.value() + '-' + TypeName.of(ProjectId.class)
                    + '-' + stringMessageId;
            Stringifier<AggregateStateId> stringifier = stringifier();

            AggregateStateId id = stringifier.reverse()
                                             .convert(stringId);

            assertNotNull(id);
            assertEquals(ANY_TYPE_URL, id.getStateType());
            assertEquals(messageId, id.getAggregateId());
        }
    }

    @Nested
    @DisplayName("fail to convert string")
    class FailToConvertString {

        @Test
        @DisplayName("which is invalid")
        void invalid() {
            String invalidId = "I'm invalid!";
            assertThrows(IllegalArgumentException.class,
                         () -> stringifier().reverse()
                                            .convert(invalidId));
        }

        @Test
        @DisplayName("with no ID type")
        void withNoIdType() {
            String invalidId = "google.protobuf/google.protobuf.Any-42";
            assertThrows(IllegalArgumentException.class,
                         () -> stringifier().reverse()
                                            .convert(invalidId));
        }

        @Test
        @DisplayName("with no state type URL")
        void withNoStateTypeUrl() {
            String invalidId = "-INT-42";
            assertThrows(IllegalArgumentException.class,
                         () -> stringifier().reverse()
                                            .convert(invalidId));
        }
    }

    @Test
    @DisplayName("convert objects back and forth")
    void convertObjectsBackAndForth() {
        Stringifier<AggregateStateId> stringifier = stringifier();
        AggregateStateId id = newMessageId();

        String stringAggregateId = stringifier.convert(id);
        assertNotNull(stringAggregateId);

        AggregateStateId restored = stringifier.reverse()
                                               .convert(stringAggregateId);
        assertEquals(id, restored);
    }

    private static Stringifier<AggregateStateId> stringifier() {
        return AggregateStateIdStringifier.getInstance();
    }
}
