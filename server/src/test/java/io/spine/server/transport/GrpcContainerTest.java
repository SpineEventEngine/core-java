/*
 * Copyright 2019, TeamDev. All rights reserved.
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
package io.spine.server.transport;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GrpcContainer should")
class GrpcContainerTest {
    private Server server;
    private GrpcContainer grpcContainer;

    @BeforeEach
    void setUp() {
        GrpcContainer.Builder builder = GrpcContainer.newBuilder();
        grpcContainer = spy(builder.build());

        server = mock(Server.class);
        doReturn(server).when(grpcContainer)
                        .createGrpcServer();
    }

    @AfterEach
    void tearDown() {
        if (!grpcContainer.isShutdown()) {
            grpcContainer.shutdown();
        }
    }

    @SuppressWarnings("MagicNumber")
    @Test
    @DisplayName("add and remove parameters from builder")
    void setParamsInBuilder() {
        int port = 60;
        GrpcContainer.Builder builder = GrpcContainer
                .newBuilder()
                .setPort(8080)
                .setPort(port);

        assertEquals(port, builder.getPort());

        int count = 3;
        List<ServerServiceDefinition> definitions = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            BindableService mockService = mock(BindableService.class);
            ServerServiceDefinition mockDefinition = ServerServiceDefinition
                    .builder(format("service-%s", i))
                    .build();
            when(mockService.bindService()).thenReturn(mockDefinition);
            definitions.add(mockDefinition);

            builder.addService(mockService);
        }

        count--;
        // Perform removal and check that the return value is builder itself.
        assertEquals(builder, builder.removeService(definitions.get(count)));

        Set<ServerServiceDefinition> serviceSet = builder.getServices();
        assertThat(serviceSet).hasSize(count);

        GrpcContainer container = builder.build();
        assertNotNull(container);
    }

    @Test
    @DisplayName("start server")
    void startServer() throws IOException {
        grpcContainer.start();

        verify(server).start();
    }

    @Test
    @DisplayName("shutdown server")
    void shutdownItself() throws IOException {
        grpcContainer.start();
        grpcContainer.shutdown();

        verify(server).shutdown();
    }

    @Test
    @DisplayName("forcefully shutdown server")
    void shutdownAndWait() throws IOException {
        grpcContainer.start();
        grpcContainer.shutdownNowAndWait();

        assertTrue(grpcContainer.isShutdown());
    }

    @Test
    @DisplayName("await termination")
    void awaitTermination() throws IOException, InterruptedException {
        grpcContainer.start();
        grpcContainer.awaitTermination();

        verify(server).awaitTermination();
    }

    @Test
    @DisplayName("stop properly upon application shutdown")
    void stopUponAppShutdown()
            throws NoSuchFieldException, IllegalAccessException, IOException {
        Class<Runtime> runtimeClass = Runtime.class;
        // Field signature: private static Runtime currentRuntime
        // Origin class: {@code java.lang.Runtime}.
        Field currentRuntimeValue = runtimeClass.getDeclaredField("currentRuntime");
        currentRuntimeValue.setAccessible(true);
        Runtime runtimeSpy = (Runtime) spy(currentRuntimeValue.get(null));
        currentRuntimeValue.set(null, runtimeSpy);

        GrpcContainer container = spy(GrpcContainer.newBuilder()
                                                   .setPort(8080)
                                                   .build());
        container.addShutdownHook();
        verify(runtimeSpy).addShutdownHook(any(Thread.class));

        container.start();
        container.getOnShutdownCallback()
                 .run();
        verify(container).shutdown();
    }

    @Nested
    @DisplayName("throw ISE if performing")
    class NotPerformTwice {

        @Test
        @DisplayName("start if container is already started")
        void start() throws IOException {
            grpcContainer.start();
            try {
                grpcContainer.start();
            } catch (IllegalStateException expected) {
                return;
            }
            fail("Exception must be thrown.");
        }

        @Test
        @DisplayName("shutdown if container is already shutdown")
        void shutdown() throws IOException {
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

    @Nested
    @DisplayName("when service is not started, throw ISE on calling")
    class ThrowWhenNotStarted {

        @Test
        @DisplayName("`awaitTermination`")
        void onAwaitTermination() {
            assertThrows(IllegalStateException.class, () -> grpcContainer.awaitTermination());
        }

        @Test
        @DisplayName("`shutdown`")
        void onShutdown() {
            assertThrows(IllegalStateException.class, () -> grpcContainer.shutdown());
        }
    }

    @Nested
    @DisplayName("assure service is")
    class AssureServiceIs {

        @Test
        @DisplayName("shutdown")
        void shutdown() throws IOException {
            grpcContainer.start();
            grpcContainer.shutdown();

            assertTrue(grpcContainer.isShutdown());
        }

        @Test
        @DisplayName("not started")
        void notStarted() {
            assertTrue(grpcContainer.isShutdown());
        }

        @Test
        @DisplayName("not shutdown")
        void notShutdown() throws IOException {
            grpcContainer.start();

            assertFalse(grpcContainer.isShutdown());
        }
    }
}
