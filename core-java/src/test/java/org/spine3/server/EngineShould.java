/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;

import java.util.List;

import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"InstanceMethodNamingConvention", "ResultOfObjectAllocationIgnored", "MagicNumber",
"ClassWithTooManyMethods", "ReturnOfNull", "ConstantConditions"})
public class EngineShould {

    @Before
    public void setUp() {
        Engine.configure(null, null);
    }


    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_not_configured_and_try_to_get_instance() {
        Engine.getInstance();
    }

    @Test
    public void return_instance_if_configured_correctly() {

        final EventStore eventStore = new EventStore(new MockStorage<String, EventRecord>());
        final CommandStore commandStore = new CommandStore(new MockStorage<String, CommandRequest>());

        Engine.configure(commandStore, eventStore);

        final Engine engine = Engine.getInstance();

        assertNotNull(engine);
        assertNotNull(engine.getCommandDispatcher());
        assertNotNull(engine.getEventBus());
    }


    private static class MockStorage<I, M extends Message> implements MessageJournal<I, M> {
        @Override
        public List<M> load(I entityId) {
            return null;
        }

        @Override
        public void store(I entityId, M message) {

        }

        @Override
        public List<M> loadSince(I entityId, Timestamp timestamp) {
            return null;
        }

        @Override
        public List<M> loadAllSince(Timestamp timestamp) {
            return null;
        }
    }
}
