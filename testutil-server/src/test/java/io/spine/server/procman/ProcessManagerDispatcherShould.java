/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.core.RejectionEnvelope;
import io.spine.server.command.TestEventFactory;
import io.spine.test.Tests;
import org.junit.Test;

import static io.spine.core.Rejections.createRejection;
import static io.spine.test.TestValues.newUuidValue;
import static org.mockito.Mockito.mock;

/**
 * @author Alexander Yevsyukov
 */
public class ProcessManagerDispatcherShould {

    @Test
    public void have_utility_ctor() {
        Tests.assertHasPrivateParameterlessCtor(ProcessManagerDispatcher.class);
    }

    @Test
    public void pass_null_tolerance_check() {
        TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(getClass());
        TestEventFactory eventFactory = TestEventFactory.newInstance(getClass());

        final Command command = requestFactory.generateCommand();
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
