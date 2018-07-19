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

package io.spine.core;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.base.Time;
import io.spine.protobuf.AnyPacker;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.core.Rejections.causedByRejection;
import static io.spine.core.Rejections.getProducer;
import static io.spine.core.Rejections.isRejection;
import static io.spine.core.Rejections.toRejection;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.TestValues.newUuidValue;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Rejections} utility class.
 *
 * <p>The test suite is located under the "server" module since actor request generation
 * and {@linkplain io.spine.server.entity.rejection.StandardRejections standard rejections} are
 * required. So we want to avoid circular dependencies between "core" and "server" modules.
 *
 * @author Alexander Yevsyukov
 */
@DisplayName("Rejections utility should")
class RejectionsTest {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private GeneratedMessageV3 rejectionMessage;
    private Command command;

    private Rejection rejection;

    @BeforeEach
    void setUp() {
        rejectionMessage = newUuidValue();
        command = requestFactory.createCommand(Time.getCurrentTime());

        TestThrowableMessage throwableMessage = (TestThrowableMessage)
                new TestThrowableMessage(rejectionMessage)
                        .initProducer(Identifier.pack(getClass().getName()));
        rejection = toRejection(throwableMessage, command);
    }

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(Rejections.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(Command.class, Command.getDefaultInstance())
                .setDefault(CommandId.class, CommandId.getDefaultInstance())
                .setDefault(ThrowableMessage.class, new TestThrowableMessage(newUuidValue()))
                .testAllPublicStaticMethods(Rejections.class);
    }

    @Test
    @DisplayName("recognize if given class represents rejection message")
    void filterRejectionClasses() {
        assertTrue(
                isRejection(
                        io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived.class)
        );
        assertFalse(isRejection(Timestamp.class));
    }

    @Test
    @DisplayName("generate rejection ID upon command ID")
    void generateRejectionIdUponCommandId() {
        CommandId commandId = Commands.generateId();
        RejectionId actual = Rejections.generateId(commandId);

        String expected = format(Rejections.REJECTION_ID_FORMAT, commandId.getUuid());
        assertEquals(expected, actual.getValue());
    }

    @Test
    @DisplayName("convert throwable message to rejection message")
    void convertThrowableMessageToRejection() {
        assertEquals(rejectionMessage, AnyPacker.unpack(rejection.getMessage()));
        assertFalse(rejection.getContext()
                             .getStacktrace()
                             .isEmpty());
        assertTrue(Timestamps.isValid(rejection.getContext()
                                               .getTimestamp()));
        Command commandFromContext = rejection.getContext()
                                              .getCommand();
        assertEquals(command, commandFromContext);
    }

    @Test
    @DisplayName("obtain rejection producer if set")
    void getRejectionProducerIfSet() {
        // We initialized producer ID as the name of this test class in setUp().
        Optional<Object> producer = Rejections.getProducer(rejection.getContext());
        assertEquals(getClass().getName(), producer.get());
    }

    @Test
    @DisplayName("return absent if rejection producer is not set")
    void returnAbsentOnEmptyProducer() {
        TestThrowableMessage freshThrowable = new TestThrowableMessage(rejectionMessage);
        Rejection freshRejection = toRejection(freshThrowable, command);
        assertFalse(getProducer(freshRejection.getContext()).isPresent());
    }

    @SuppressWarnings({
            "NewExceptionWithoutArguments" /* No need to have a message for this test. */,
            "SerializableInnerClassWithNonSerializableOuterClass" /* Does not refer anything. */
    })
    @Test
    @DisplayName("tell if RuntimeException was caused by command rejection")
    void recognizeExceptionCausedByRejection() {
        assertFalse(causedByRejection(new RuntimeException()));
        ThrowableMessage throwableMessage = new ThrowableMessage(Time.getCurrentTime()) {
            private static final long serialVersionUID = 0L;
        };
        assertTrue(causedByRejection(new IllegalStateException(throwableMessage)));

        // Check that root cause is analyzed.
        assertTrue(causedByRejection(
                new RuntimeException(new IllegalStateException(throwableMessage))));
    }

    /**
     * Sample {@code ThrowableMessage} class used for test purposes only.
     */
    private static class TestThrowableMessage extends ThrowableMessage {

        private static final long serialVersionUID = 0L;

        private TestThrowableMessage(GeneratedMessageV3 message) {
            super(message);
        }
    }
}
