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

package io.spine.server.procman;

import com.google.common.testing.NullPointerTester;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.core.RejectionEnvelope;
import io.spine.server.command.TestEventFactory;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.core.Rejections.createRejection;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.TestValues.newUuidValue;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.mockito.Mockito.mock;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("ProcessManagerDispatcher utility should")
class ProcessManagerDispatcherTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(ProcessManagerDispatcher.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(getClass());
        TestEventFactory eventFactory = TestEventFactory.newInstance(getClass());

        Command command = requestFactory.generateCommand();
        new NullPointerTester()
                .setDefault(CommandEnvelope.class,
                            CommandEnvelope.of(command))
                .setDefault(EventEnvelope.class,
                            EventEnvelope.of(eventFactory.createEvent(newUuidValue())))
                .setDefault(RejectionEnvelope.class,
                            RejectionEnvelope.of(createRejection(newUuidValue(), command)))
                .setDefault(ProcessManager.class, mock(ProcessManager.class))
                .testAllPublicStaticMethods(ProcessManagerDispatcher.class);
    }
}
