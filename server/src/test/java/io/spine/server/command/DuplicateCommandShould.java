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

package io.spine.server.command;

import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Status;
import io.spine.core.TenantId;
import io.spine.server.command.given.DuplicateCommandTestEnv.TestClient;
import io.spine.server.command.given.DuplicateCommandTestEnv.TestServer;
import io.spine.server.commandbus.DuplicateCommandException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.spine.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT;
import static io.spine.server.command.given.DuplicateCommandTestEnv.SERVICE_HOST;
import static io.spine.server.command.given.DuplicateCommandTestEnv.command;
import static io.spine.server.command.given.DuplicateCommandTestEnv.createProject;
import static io.spine.server.command.given.DuplicateCommandTestEnv.newTenantId;
import static io.spine.server.command.given.DuplicateCommandTestEnv.runServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mykhailo Drachuk
 */
public class DuplicateCommandShould {

    private TestClient client;
    private TestServer server;

    @Before
    public void setUp() throws Exception {
        server = new TestServer(DEFAULT_CLIENT_SERVICE_PORT);
        runServer(server);

        client = new TestClient(SERVICE_HOST, DEFAULT_CLIENT_SERVICE_PORT);
    }

    @After
    public void tearDown() throws Exception {
        client.shutdown();
        server.shutdown();
    }

    @Test
    public void not_be_acknowledged_on_client_when_not_sent() {
        final TenantId tenantId = newTenantId();

        final Command command = command(createProject(), tenantId);

        final Ack ack = client.post(command);

        final Status status = ack.getStatus();
        assertTrue(status.hasOk());
    }

    @Test
    public void be_acknowledged_on_client_when_posted_to_an_aggregate() {
        final TenantId tenantId = newTenantId();

        final Command command = command(createProject(), tenantId);

        client.post(command);
        final Ack ack = client.post(command);

        final Status status = ack.getStatus();
        final Error error = status.getError();
        final String errorType = error.getType();
        final String expectedErrorType = DuplicateCommandException.class.getCanonicalName();
        assertEquals(expectedErrorType, errorType);
    }
}
