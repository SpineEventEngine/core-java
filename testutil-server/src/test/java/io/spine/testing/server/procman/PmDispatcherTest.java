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

package io.spine.testing.server.procman;

import com.google.common.testing.NullPointerTester;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.server.procman.ProcessManager;
import io.spine.testing.UtilityClassTest;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.given.entity.event.TuProjectCreated;
import org.junit.jupiter.api.DisplayName;

import static org.mockito.Mockito.mock;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("ProcessManagerDispatcher utility should")
class PmDispatcherTest extends UtilityClassTest<PmDispatcher> {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());
    private final TestEventFactory eventFactory = TestEventFactory.newInstance(getClass());

    PmDispatcherTest() {
        super(PmDispatcher.class);
    }

    @Override
    protected void setDefaults(NullPointerTester tester) {
        Command command = requestFactory.generateCommand();
        Event event = eventFactory.createEvent(TuProjectCreated.getDefaultInstance());
        tester.setDefault(CommandEnvelope.class,
                          CommandEnvelope.of(command))
              .setDefault(EventEnvelope.class,
                          EventEnvelope.of(event))
              .setDefault(ProcessManager.class, mock(ProcessManager.class));
    }
}
