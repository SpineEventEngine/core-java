/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.command;

import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.client.ActorRequestFactory;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.server.Environment;

import static org.junit.Assert.assertEquals;
import static org.spine3.test.Tests.newUuidValue;

/**
 * @author Alexander Yevsyukov
 */
public class CommandHandlingEntityShould {

    private final ActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    /** The object we test. */
    private HandlingEntity entity;

    @Before
    public void setUp() {
        entity = new HandlingEntity(1L);
    }

    @Test
    public void set_version_when_creating_mismatches() {
        final int version = entity.getVersion().getNumber();

        assertEquals(version, entity.expectedDefault(msg(), msg()).getVersion());
        assertEquals(version, entity.expectedNotDefault(msg()).getVersion());
        assertEquals(version, entity.expectedNotDefault(msg(), msg()).getVersion());
        assertEquals(version, entity.unexpectedValue(msg(), msg(), msg()).getVersion());

        assertEquals(version, entity.expectedEmpty(str(), str()).getVersion());
        assertEquals(version, entity.expectedNotEmpty(str()).getVersion());
        assertEquals(version, entity.unexpectedValue(str(), str(), str()).getVersion());
    }

    @Test(expected = IllegalStateException.class)
    public void do_not_allow_calling_dispatchForTest_from_production() {
        final Environment environment = Environment.getInstance();
        try {
            // Simulate the production mode.
            environment.setToProduction();

            final Command cmd = requestFactory.command().create(msg());
            entity.dispatchForTest(CommandEnvelope.of(cmd));

        } finally {
            environment.setToTests();
        }
    }

    /**
     * @return generated {@code StringValue} based on generated UUID
     */
    private static StringValue msg() {
        return newUuidValue();
    }

    /**
     * @return generated {@code String} based on generated UUID
     */
    private static String str() {
        return msg().getValue();
    }


    private static class HandlingEntity extends CommandHandlingEntity<Long, StringValue> {
        private HandlingEntity(Long id) {
            super(id);
        }
    }
}
