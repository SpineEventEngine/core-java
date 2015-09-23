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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.storage.memory.InMemoryStorageFactory;

import static org.junit.Assert.assertNotNull;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class EngineShould {

    @Before
    public void setUp() {
        Engine.start(InMemoryStorageFactory.getInstance());
    }

    @After
    public void tearDown() {
        Engine.stop();
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_not_configured_and_try_to_get_instance() {
        Engine.stop();

        Engine.getInstance();
    }

    @Test
    public void return_instance_if_configured_correctly() {
        final Engine engine = Engine.getInstance();
        assertNotNull(engine);
    }

    @Test
    public void return_EventBus() {
        assertNotNull(Engine.getInstance().getEventBus());
    }

    @Test
    public void return_CommandDispatcher() {
        assertNotNull(Engine.getInstance().getCommandDispatcher());
    }
}
