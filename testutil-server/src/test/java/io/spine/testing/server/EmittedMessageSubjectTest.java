/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.testing.server;

import com.google.common.truth.TruthFailureSubject;
import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.protobuf.Any;
import io.spine.core.MessageWithContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.ExpectFailure.assertThat;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.server.EmittedMessageSubject.MESSAGE_COUNT_FACT_KEY;
import static io.spine.testing.server.EmittedMessageSubject.REQUESTED_INDEX_FACT_KEY;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.generate;

abstract class EmittedMessageSubjectTest<S extends EmittedMessageSubject<S, M, ?>,
                                         M extends MessageWithContext>
        extends SubjectTest<S, Iterable<M>> {

    abstract S assertWithSubjectThat(Iterable<M> messages);

    abstract M createMessage();

    private Iterable<M> messages(int messageCount) {
        return generate(this::createMessage)
                .limit(messageCount)
                .collect(toList());
    }

    @Test
    @DisplayName("check message count")
    void checkSize() {
        int messageCount = 42;
        Iterable<M> messages = messages(messageCount);
        assertWithSubjectThat(messages).hasSize(messageCount);
        AssertionError error = expectFailure(
                whenTesting -> whenTesting.that(messages)
                                          .hasSize(0)
        );
        TruthFailureSubject assertError = assertThat(error);
        assertError.factValue(EXPECTED)
                   .isEqualTo(String.valueOf(0));
        assertError.factValue(BUT_WAS)
                   .isEqualTo(String.valueOf(messageCount));
    }

    @Test
    @DisplayName("check if there is no messages")
    void checkIfAbsent() {
        int messageCount = 0;
        Iterable<M> messages = messages(messageCount);
        assertWithSubjectThat(messages).isEmpty();
        expectSomeFailure(whenTesting -> whenTesting.that(messages)
                                                    .isNotEmpty());
    }

    @Test
    @DisplayName("check if there are some messages")
    void checkIfPresent() {
        int messageCount = 1;
        Iterable<M> messages = messages(messageCount);
        assertWithSubjectThat(messages).isNotEmpty();
        expectSomeFailure(whenTesting -> whenTesting.that(messages)
                                                    .isEmpty());
    }

    @Test
    @DisplayName("retrieve a subject of an emitted message by its index")
    void retrieveMessage() {
        int messageCount = 3;
        Iterable<M> messages = messages(messageCount);
        ProtoSubject<?, ?> protoSubject = assertWithSubjectThat(messages).message(2);
        assertThat(protoSubject).isNotNull();
        protoSubject.isNotEqualToDefaultInstance();
        protoSubject.isNotInstanceOf(Any.class);
        protoSubject.isNotInstanceOf(MessageWithContext.class);
    }

    @Test
    @DisplayName("check the message count when obtaining a ProtoSubject")
    void failOnIndexOutOfBounds() {
        int messageCount = 5;
        Iterable<M> messages = messages(messageCount);
        int index = 13;
        @SuppressWarnings("CheckReturnValue")
        AssertionError error = expectFailure(whenTesting -> whenTesting.that(messages)
                                                                       .message(index));
        TruthFailureSubject assertError = assertThat(error);
        assertError.factValue(MESSAGE_COUNT_FACT_KEY)
                   .isEqualTo(String.valueOf(messageCount));
        assertError.factValue(REQUESTED_INDEX_FACT_KEY)
                   .isEqualTo(String.valueOf(index));
    }
}
