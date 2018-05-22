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

import com.google.common.base.Optional;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.base.Time;
import io.spine.client.TestActorRequestFactory;
import io.spine.protobuf.AnyPacker;
import org.junit.Before;
import org.junit.Test;

import static io.spine.core.Rejections.causedByRejection;
import static io.spine.core.Rejections.getProducer;
import static io.spine.core.Rejections.isRejection;
import static io.spine.core.Rejections.toRejection;
import static io.spine.test.TestValues.newUuidValue;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link Rejections} utility class.
 *
 * <p>The test suite is located under the "server" module since actor request generation
 * and {@linkplain io.spine.server.entity.rejection.StandardRejections standard rejections} are
 * required. So we want to avoid circular dependencies between "core" and "server" modules.
 *
 * @author Alexander Yevsyukov
 */
public class RejectionsShould {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private GeneratedMessageV3 rejectionMessage;
    private Command command;

    private Rejection rejection;
    @Before
    public void setUp() {
        rejectionMessage = newUuidValue();
        command = requestFactory.createCommand(Time.getCurrentTime());

        TestThrowableMessage throwableMessage = (TestThrowableMessage)
                new TestThrowableMessage(rejectionMessage)
                        .initProducer(Identifier.pack(getClass().getName()));
        rejection = toRejection(throwableMessage, command);
    }

    @Test
    public void have_utility_ctor() {
        assertHasPrivateParameterlessCtor(Rejections.class);
    }

    @Test
    public void filter_rejection_classes() {
        assertTrue(
                isRejection(io.spine.server.entity.rejection.StandardRejections.EntityAlreadyArchived.class)
        );
        assertFalse(isRejection(Timestamp.class));
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Command.class, Command.getDefaultInstance())
                .setDefault(CommandId.class, CommandId.getDefaultInstance())
                .setDefault(ThrowableMessage.class, new TestThrowableMessage(newUuidValue()))
                .testAllPublicStaticMethods(Rejections.class);
    }

    @Test
    public void generate_rejection_id_upon_command_id() {
        final CommandId commandId = Commands.generateId();
        final RejectionId actual = Rejections.generateId(commandId);

        final String expected = format(Rejections.REJECTION_ID_FORMAT, commandId.getUuid());
        assertEquals(expected, actual.getValue());
    }

    @Test
    public void convert_throwable_message_to_rejection_message() {
        assertEquals(rejectionMessage, AnyPacker.unpack(rejection.getMessage()));
        assertFalse(rejection.getContext()
                             .getStacktrace()
                             .isEmpty());
        assertTrue(Timestamps.isValid(rejection.getContext()
                                               .getTimestamp()));
        final Command commandFromContext = rejection.getContext()
                                                    .getCommand();
        assertEquals(command, commandFromContext);
    }

    @Test
    public void obtain_rejection_producer_if_set() {
        // We initialized producer ID as the name of this test class in setUp().
        final Optional<Object> producer = Rejections.getProducer(rejection.getContext());
        assertEquals(getClass().getName(), producer.get());
    }

    @Test
    public void return_empty_optional_if_producer_not_set() {
        final TestThrowableMessage freshThrowable = new TestThrowableMessage(rejectionMessage);
        final Rejection freshRejection = toRejection(freshThrowable, command);
        assertFalse(getProducer(freshRejection.getContext()).isPresent());
    }

    @SuppressWarnings({
            "NewExceptionWithoutArguments" /* No need to have a message for this test. */,
            "SerializableInnerClassWithNonSerializableOuterClass" /* Does not refer anything. */
    })
    @Test
    public void tell_if_RuntimeException_was_called_by_command_rejection() {
        assertFalse(causedByRejection(new RuntimeException()));
        final ThrowableMessage throwableMessage = new ThrowableMessage(Time.getCurrentTime()) {
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
