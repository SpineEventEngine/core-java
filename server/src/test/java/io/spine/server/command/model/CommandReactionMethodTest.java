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

package io.spine.server.command.model;

import com.google.common.truth.IterableSubject;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.server.command.model.given.reaction.ReOneParam;
import io.spine.server.command.model.given.reaction.ReOptionalResult;
import io.spine.server.command.model.given.reaction.ReTwoParams;
import io.spine.server.command.model.given.reaction.TestCommandReactor;
import io.spine.server.entity.PropagationOutcome;
import io.spine.server.entity.Success;
import io.spine.server.event.EventReceiver;
import io.spine.server.type.EventEnvelope;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.ProjectId;
import io.spine.test.command.event.CmdProjectCreated;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("InnerClassMayBeStatic")
@DisplayName("CommandReactionMethod should")
class CommandReactionMethodTest {

    private static final
    CommandReactionSignature signature = new CommandReactionSignature();

    private static void assertValid(Method rawMethod, boolean isValid) {
        assertThat(signature.matches(rawMethod)).isEqualTo(isValid);
    }

    @Nested
    @DisplayName("consider command reaction valid with")
    class MethodArguments {

        @Test
        @DisplayName("one event message parameter")
        void oneParam() {
            Method method = new ReOneParam().getMethod();
            assertValid(method, true);
        }

        @Test
        @DisplayName("event message and context")
        void twoParams() {
            Method method = new ReTwoParams().getMethod();
            assertValid(method, true);
        }
    }

    @Nested
    @DisplayName("support Message return type")
    class MessageReturn {

        private EventReceiver target;
        private Method rawMethod;
        private CommandReactionMethod method;
        private ProjectId id;

        @BeforeEach
        void setUp() {
            target = new ReOneParam();
            rawMethod = ((TestCommandReactor) target).getMethod();
            Optional<CommandReactionMethod> result = signature.create(rawMethod);
            assertTrue(result.isPresent());
            this.method = result.get();
            id = ProjectId.newBuilder()
                          .setId(newUuid())
                          .build();
        }

        @Test
        @DisplayName("in predicate")
        void predicate() {
            assertValid(rawMethod, true);
        }

        @Test
        @DisplayName("in factory")
        void factory() {
            assertThat(rawMethod).isNotNull();
        }

        @Test
        @DisplayName("when returning value")
        void returnValue() {
            CmdProjectCreated message = createEvent(id);
            PropagationOutcome outcome = method.invoke(target, envelope(message));
            assertResult(outcome, this.id);
        }
    }

    @Nested
    @DisplayName("support Optional return value")
    class OptionalReturn {

        private final Method rawMethod = new ReOptionalResult().getMethod();

        @SuppressWarnings("OptionalGetWithoutIsPresent") // OK as we surely get the result
        private final CommandReactionMethod method = signature.create(rawMethod)
                                                              .get();

        private EventReceiver target;
        private ProjectId id;

        @BeforeEach
        void setUp() {
            target = new ReOptionalResult();
            id = ProjectId.newBuilder()
                          .setId(newUuid())
                          .build();
        }

        @Test
        @DisplayName("in predicate")
        void inPredicate() {
            boolean result = signature.matches(rawMethod);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("in factory")
        void inFactory() {
            assertThat(method).isNotNull();
        }

        @Test
        @DisplayName("when returning value")
        void returnValue() {
            ProjectId givenId = this.id;
            CmdProjectCreated message = createEvent(givenId);

            PropagationOutcome outcome = method.invoke(target, envelope(message));

            assertResult(outcome, givenId);
        }

        @Test
        @DisplayName("when returning Optional.empty()")
        void returnEmpty() {
            CmdProjectCreated message = CmdProjectCreated
                    .newBuilder()
                    .setProjectId(id)
                    .setInitialize(false) // This will make the method return `Optional.empty()`.
                    .build();

            PropagationOutcome outcome = method.invoke(target, envelope(message));

            assertThat(outcome.getSuccess().getProducedCommands().getCommandList()).isEmpty();
        }
    }

    private static CmdProjectCreated createEvent(ProjectId givenId) {
        return CmdProjectCreated
                .newBuilder()
                .setProjectId(givenId)
                .setInitialize(true) // This will make the method return Optional with value.
                .build();
    }

    /**
     * Asserts that the result has a message with correct type and passed field value.
     */
    private static void assertResult(PropagationOutcome outcome, ProjectId id) {
        CmdAddTask expected = CmdAddTask
                .newBuilder()
                .setProjectId(id)
                .build();
        assertTrue(outcome.hasSuccess());
        Success success = outcome.getSuccess();
        assertTrue(success.hasProducedCommands());
        List<Command> commands = success.getProducedCommands()
                                    .getCommandList();
        List<CommandMessage> commandMessages = commands
                .stream()
                .map(Command::enclosedMessage)
                .collect(toList());
        IterableSubject assertThat = assertThat(commandMessages);
        assertThat.hasSize(1);
        assertThat.containsExactly(expected);
    }

    private static EventEnvelope envelope(EventMessage eventMessage) {
        TestEventFactory factory = TestEventFactory.newInstance(CommandReactionMethodTest.class);
        Event event = factory.createEvent(eventMessage);
        EventEnvelope envelope = EventEnvelope.of(event);
        return envelope;
    }
}
