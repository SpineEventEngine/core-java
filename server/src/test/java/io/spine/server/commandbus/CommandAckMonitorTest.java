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

package io.spine.server.commandbus;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.Commands;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.grpc.StreamObservers;
import io.spine.server.bus.Buses;
import io.spine.server.entity.rejection.CannotModifyArchivedEntity;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.type.CommandEnvelope;
import io.spine.system.server.CommandAcknowledged;
import io.spine.system.server.CommandErrored;
import io.spine.system.server.MemoizingWriteSide;
import io.spine.system.server.NoOpSystemWriteSide;
import io.spine.test.commandbus.CmdBusStartProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.protobuf.AnyPacker.pack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("InnerClassMayBeStatic")
@DisplayName("CommandAckMonitor should")
class CommandAckMonitorTest {

    @Test
    @DisplayName("not accept null arguments in Builder")
    void rejectNulls() {
        new NullPointerTester()
                .testInstanceMethods(CommandAckMonitor.newBuilder(), PACKAGE);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") // Builder method invocations.
    @Nested
    @DisplayName("not build without")
    class RequireParameter {

        private CommandAckMonitor.Builder builder;

        @BeforeEach
        void setUp() {
            builder = CommandAckMonitor.newBuilder();
        }

        @Test
        @DisplayName("delegate StreamObserver")
        void delegate() {
            builder.setTenantId(TenantId.getDefaultInstance())
                   .setSystemWriteSide(NoOpSystemWriteSide.INSTANCE);
            assertFailsToBuild();
        }

        @Test
        @DisplayName("tenant ID")
        void tenant() {
            builder.setDelegate(noOpObserver())
                   .setSystemWriteSide(NoOpSystemWriteSide.INSTANCE);
            assertFailsToBuild();
        }

        @Test
        @DisplayName("system write side")
        void system() {
            builder.setDelegate(noOpObserver())
                   .setTenantId(TenantId.getDefaultInstance());
            assertFailsToBuild();
        }

        private void assertFailsToBuild() {
            assertThrows(NullPointerException.class, builder::build);
        }
    }

    @Nested
    @DisplayName("if Ack contains")
    class PostSystemCommands {

        private CommandAckMonitor monitor;
        private MemoizingWriteSide writeSide;

        private CommandId commandId;

        @BeforeEach
        void setUp() {
            writeSide = MemoizingWriteSide.singleTenant();
            monitor = CommandAckMonitor
                    .newBuilder()
                    .setDelegate(noOpObserver())
                    .setSystemWriteSide(writeSide)
                    .setTenantId(TenantId.getDefaultInstance())
                    .build();
            commandId = Commands.generateId();
        }

        @Test
        @DisplayName("OK marker, post MarkCommandAsAcknowledged")
        void onOk() {
            Ack ack = okAck(commandId);
            monitor.onNext(ack);

            Message lastSeenEvent = writeSide.lastSeenEvent()
                                             .message();

            assertThat(lastSeenEvent).isInstanceOf(CommandAcknowledged.class);

            CommandAcknowledged actualEvent = (CommandAcknowledged) lastSeenEvent;
            assertThat(actualEvent.getId()).isEqualTo(commandId);
        }

        @Test
        @DisplayName("error, post MarkCommandAsErrored")
        void onError() {
            Ack ack = errorAck(commandId);
            monitor.onNext(ack);

            Message lastSeenEvent = writeSide.lastSeenEvent()
                                             .message();

            assertThat(lastSeenEvent).isInstanceOf(CommandErrored.class);

            CommandErrored actualEvent = (CommandErrored) lastSeenEvent;

            assertThat(actualEvent.getId()).isEqualTo(commandId);
            assertThat(actualEvent.getError()).isEqualTo(ack.getStatus()
                                                            .getError());
        }

        @Test
        @DisplayName("rejection, throw an exception")
        void onRejection() {
            Ack ack = rejectionAck(commandId);
            assertThrows(IllegalArgumentException.class, () -> monitor.onNext(ack));
        }
    }

    @Nested
    @DisplayName("delegate to a given observer")
    class DelegateCalls {

        private MemoizingObserver<Ack> delegate;
        private CommandAckMonitor monitor;
        private CommandId commandId;

        @BeforeEach
        void setUp() {
            delegate = StreamObservers.memoizingObserver();
            monitor = CommandAckMonitor
                    .newBuilder()
                    .setTenantId(TenantId.getDefaultInstance())
                    .setSystemWriteSide(NoOpSystemWriteSide.INSTANCE)
                    .setDelegate(delegate)
                    .build();
            commandId = Commands.generateId();
        }

        @Test
        @DisplayName("onNext(OK)")
        void nextOk() {
            Ack ack = okAck(commandId);
            checkOnNext(ack);
        }

        @Test
        @DisplayName("onNext(Error)")
        void nextError() {
            Ack ack = errorAck(commandId);
            checkOnNext(ack);
        }

        @Test
        @DisplayName("onNext(Rejection)")
        void nextRejection() {
            Ack ack = rejectionAck(commandId);
            checkOnNext(ack);
        }

        @Test
        @DisplayName("onError(...)")
        void error() {
            Throwable error = new Throwable();
            monitor.onError(error);

            assertEquals(error, delegate.getError());
        }

        @SuppressWarnings("DuplicateStringLiteralInspection") // Method name used in other scope.
        @Test
        @DisplayName("onCompleted()")
        void complete() {
            monitor.onCompleted();

            assertTrue(delegate.isCompleted());
        }

        private void checkOnNext(Ack ack) {
            try {
                monitor.onNext(ack);
            } catch (RuntimeException ignored) {
                // May throw an exception after delegating the call.
            }

            Ack received = delegate.firstResponse();
            assertEquals(ack, received);
        }
    }

    private static Ack okAck(CommandId commandId) {
        return Buses.acknowledge(commandId);
    }

    private static Ack errorAck(CommandId commandId) {
        Error error = Error
                .newBuilder()
                .setCode(42)
                .setMessage("Wrong question")
                .build();
        return Buses.reject(commandId, error);
    }

    private static Ack rejectionAck(CommandId commandId) {
        CommandMessage commandMessage = CmdBusStartProject
                .newBuilder()
                .setId(commandId.getUuid())
                .build();
        Command command = Command
                .newBuilder()
                .setId(commandId)
                .setMessage(pack(commandMessage))
                .build();
        CommandEnvelope envelope = CommandEnvelope.of(command);

        Any entityId = Identifier.pack(CommandAckMonitorTest.class.getSimpleName());
        CannotModifyArchivedEntity rejectionThrowable = CannotModifyArchivedEntity
                .newBuilder()
                .setEntityId(entityId)
                .build();
        RuntimeException wrapperThrowable = new RuntimeException(rejectionThrowable);
        RejectionEnvelope rejection = RejectionEnvelope.from(envelope, wrapperThrowable);

        return Buses.reject(commandId, rejection);
    }
}
