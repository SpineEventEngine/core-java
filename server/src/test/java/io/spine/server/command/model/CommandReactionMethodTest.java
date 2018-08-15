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

package io.spine.server.command.model;

import com.google.common.truth.IterableSubject;
import io.spine.core.EventContext;
import io.spine.server.command.model.given.reaction.ReOptionalCommandOnEvent;
import io.spine.server.event.EventReceiver;
import io.spine.server.model.MethodFactory;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.ProjectId;
import io.spine.test.command.event.CmdProjectCreated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.server.command.model.given.MethodHarness.getMethod;

@SuppressWarnings("InnerClassMayBeStatic")
@DisplayName("CommandReactionMethod should")
class CommandReactionMethodTest {

    private final MethodFactory<CommandReactionMethod> factory = CommandReactionMethod.factory();

    @Nested
    @DisplayName("support Optional return value")
    class OptionalReturn {

        private final Method rawMethod = getMethod(ReOptionalCommandOnEvent.class);
        private final CommandReactionMethod method = factory.create(rawMethod);

        private EventReceiver target;
        private ProjectId id;

        @BeforeEach
        void setUp() {
            target = new ReOptionalCommandOnEvent();
            id = ProjectId.newBuilder()
                          .setId(newUuid())
                          .build();
        }

        @Test
        @DisplayName("in factory")
        void inFactory() {
            assertThat(method).isNotNull();
        }

        @Test
        @DisplayName("in predicate")
        void inPredicate() {
            boolean result = factory.getPredicate()
                                    .test(rawMethod);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("when returning value")
        void returnValue() {
            CmdProjectCreated message = CmdProjectCreated
                    .newBuilder()
                    .setProjectId(id)
                    .setInitialize(true) // This will make the method return value.
                    .build();

            CommandingMethod.Result result =
                    method.invoke(target, message, EventContext.getDefaultInstance());

            CmdAddTask expected = CmdAddTask
                    .newBuilder()
                    .setProjectId(id)
                    .build();

            IterableSubject assertThat = assertThat(result.asMessages());
            assertThat.hasSize(1);
            assertThat.containsExactly(expected);
        }

        @Test
        @DisplayName("when returning Optional.empty()")
        void returnEmpty() {
            CmdProjectCreated message = CmdProjectCreated
                    .newBuilder()
                    .setProjectId(id)
                    .setInitialize(false) // This will make the method return `Optional.empty()`.
                    .build();

            CommandingMethod.Result result =
                    method.invoke(target, message, EventContext.getDefaultInstance());

            IterableSubject assertThat = assertThat(result.asMessages());
            assertThat.isEmpty();
        }
    }
}
