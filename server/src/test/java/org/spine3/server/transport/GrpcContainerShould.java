/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
package org.spine3.server.transport;

import io.grpc.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Alex Tymchenko
 */
public class GrpcContainerShould {
    private Server server;
    private GrpcContainer grpcContainer;


    @Before
    public void setUp() {
        final GrpcContainer.Builder builder = GrpcContainer.newBuilder();
        grpcContainer = spy(builder.build());

        server = mock(Server.class);
        doReturn(server).when(grpcContainer).createGrpcServer();
    }

    @After
    public void tearDown() {
        if (!grpcContainer.isShutdown()) {
            grpcContainer.shutdown();
        }
    }

    @Test
    public void start_server() throws IOException {
        grpcContainer.start();

        verify(server).start();
    }

    @Test
    public void throw_exception_if_started_already() throws IOException {
        grpcContainer.start();
        try {
            grpcContainer.start();
        } catch (IllegalStateException expected) {
            return;
        }
        fail("Exception must be thrown.");
    }

    @Test
    public void await_termination() throws IOException, InterruptedException {
        grpcContainer.start();
        grpcContainer.awaitTermination();

        verify(server).awaitTermination();
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_call_await_termination_on_not_started_service() {
        grpcContainer.awaitTermination();
    }

    @Test
    public void assure_service_is_shutdown() throws IOException {
        grpcContainer.start();
        grpcContainer.shutdown();

        assertTrue(grpcContainer.isShutdown());
    }

    @Test
    public void assure_service_was_not_started() throws IOException {
        assertTrue(grpcContainer.isShutdown());
    }

    @Test
    public void assure_service_is_not_shut_down() throws IOException {
        grpcContainer.start();

        assertFalse(grpcContainer.isShutdown());
    }

    @Test
    public void shutdown_itself() throws IOException, InterruptedException {
        grpcContainer.start();
        grpcContainer.shutdown();

        verify(server).shutdown();
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_call_shutdown_on_not_started_service() {
        grpcContainer.shutdown();
    }

    @Test
    public void throw_exception_if_shutdown_already() throws IOException {
        grpcContainer.start();
        grpcContainer.shutdown();
        try {
            grpcContainer.shutdown();
        } catch (IllegalStateException expected) {
            return;
        }
        fail("Expected an exception.");
    }

}
