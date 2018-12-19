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

package io.spine.server.delivery;

import io.spine.server.ServerEnvironment;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.server.transport.memory.SingleThreadInMemTransportFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static io.spine.testing.server.model.ModelTests.dropAllModels;

/**
 * An abstract base for message delivery tests.
 *
 * <p>Allows to customize the {@link ServerEnvironment} for tests. Such a customization is really
 * helpful for tests, that use cloud-based environments.
 *
 * @author Alex Tymchenko
 */
public abstract class AbstractMessageDeliveryTest {

    @BeforeEach
    protected void setUp() {
        dropAllModels();
        switchToShardingWithTransport(getTransport());
    }

    @AfterEach
    protected void tearDown() {
        switchToShardingWithTransport(InMemoryTransportFactory.newInstance());
    }

    /**
     * Switches the current {@link ServerEnvironment} to the sharding based on the given transport
     *
     * @param transport the transport to use for the sharding in the current server environment
     */
    protected void switchToShardingWithTransport(TransportFactory transport) {
        Sharding inProcessSharding = newSharding(transport);
        ServerEnvironment.getInstance()
                         .replaceSharding(inProcessSharding);
    }

    /**
     * Creates an instance of {@link TransportFactory} to use for the delivery tests.
     *
     * @return an instance of {@link TransportFactory} to use in tests
     */
    protected TransportFactory getTransport() {
        return SingleThreadInMemTransportFactory.newInstance();
    }

    /**
     * Creates an instance of {@link Sharding} to use for the delivery tests.
     *
     * @return an instance of {@link Sharding} to use in tests
     */
    protected Sharding newSharding(TransportFactory transport) {
        return new InProcessSharding(transport);
    }
}
