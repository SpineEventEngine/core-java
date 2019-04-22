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

import com.google.common.collect.ImmutableList;
import com.google.common.truth.TruthFailureSubject;
import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.protobuf.Any;
import io.spine.base.SerializableMessage;
import io.spine.core.MessageWithContext;
import io.spine.testing.server.EmittedMessageSubject.FactKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static com.google.common.truth.ExpectFailure.assertThat;
import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.generate;

abstract class EmittedMessageSubjectTest<S extends EmittedMessageSubject<S, W, M>,
                                         W extends MessageWithContext,
                                         M extends SerializableMessage>
        extends SubjectTest<S, Iterable<W>> {

    abstract S assertWithSubjectThat(Iterable<W> messages);

    abstract W createMessage();

    abstract W createAnotherMessage();

    private Iterable<W> messages(int messageCount) {
        Supplier<W> supplier = this::createMessage;
        return messages(messageCount, supplier);
    }

    private Iterable<W> messages(int messageCount, Supplier<W> supplier) {
        return generate(supplier)
                .limit(messageCount)
                .collect(toList());
    }

    @Test
    @DisplayName("check message count")
    void checkSize() {
        int messageCount = 42;
        Iterable<W> messages = messages(messageCount);
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
        Iterable<W> messages = messages(messageCount);
        assertWithSubjectThat(messages).isEmpty();
        expectSomeFailure(whenTesting -> whenTesting.that(messages)
                                                    .isNotEmpty());
    }

    @Test
    @DisplayName("check if there are some messages")
    void checkIfPresent() {
        int messageCount = 1;
        Iterable<W> messages = messages(messageCount);
        assertWithSubjectThat(messages).isNotEmpty();
        expectSomeFailure(whenTesting -> whenTesting.that(messages)
                                                    .isEmpty());
    }

    @Test
    @DisplayName("retrieve a subject of an emitted message by its index")
    void retrieveMessage() {
        int messageCount = 3;
        Iterable<W> messages = messages(messageCount);
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
        Iterable<W> messages = messages(messageCount);
        int index = 13;
        @SuppressWarnings("CheckReturnValue")
        AssertionError error = expectFailure(whenTesting -> whenTesting.that(messages)
                                                                       .message(index));
        TruthFailureSubject assertError = assertThat(error);
        assertError.factValue(FactKey.MESSAGE_COUNT.value())
                   .isEqualTo(String.valueOf(messageCount));
        assertError.factValue(FactKey.REQUESTED_INDEX.value())
                   .isEqualTo(String.valueOf(index));
    }

    @Test
    @DisplayName("obtain derived subject with messages of type")
    void subSubjectByType() {
        int messageCount = 3;
        int otherMessageCount = 2;
        Iterable<W> outerObjects = ImmutableList
                .<W>builder()
                .addAll(messages(messageCount))
                .addAll(messages(otherMessageCount, this::createAnotherMessage))
                .build();

        Class<M> type = typeOf(createMessage());
        Class<M> anotherType = typeOf(createAnotherMessage());

        S subject = assertWithSubjectThat(outerObjects);

        S subSubject = subject.withType(type);
        subSubject.hasSize(messageCount);

        S anotherSubSubject = subject.withType(anotherType);
        anotherSubSubject.hasSize(otherMessageCount);
    }

    @Test
    @DisplayName("fail when trying to obtain filtered sub-subject over null actual")
    void failWithNull() {
        Class<M> type = typeOf(createMessage());
        @SuppressWarnings("CheckReturnValue") /* The call to `withType()` should fail,
            we don't need its result. */
        AssertionError error = expectFailure(whenTesting -> whenTesting.that(null)
                                                                       .withType(type));
        TruthFailureSubject assertError = assertThat(error);
        assertError.factValue(FactKey.ACTUAL.value())
                   .isEqualTo("null");
    }

    private Class<M> typeOf(W outerObject) {
        @SuppressWarnings("unchecked") Class<M> /* The cast is protected by matching outer type
            (such as `Event` or `Command`) to corresponding enclosed message type (such as
            `EventMessage` or `CommandMessage`) in the generic parameters of the derived classes. */
        result = (Class<M>) outerObject.enclosedMessage().getClass();
        return result;
    }
}
