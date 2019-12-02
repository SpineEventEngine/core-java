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

package io.spine.testing.server.blackbox;

import com.google.protobuf.Empty;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.core.CommandId;
import io.spine.core.EventId;
import io.spine.core.MessageId;
import io.spine.system.server.AggregateHistoryCorrupted;
import io.spine.system.server.CannotDispatchDuplicateCommand;
import io.spine.system.server.CannotDispatchDuplicateEvent;
import io.spine.system.server.ConstraintViolated;
import io.spine.system.server.HandlerFailedUnexpectedly;
import io.spine.system.server.RoutingFailed;
import io.spine.testing.server.blackbox.command.BbCreateProject;
import io.spine.testing.server.blackbox.event.BbProjectCreated;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Errors.causeOf;
import static io.spine.base.Errors.fromThrowable;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Identifier.pack;

@DisplayName("`Dashboard` should")
class DashboardTest {

    private ByteArrayOutputStream output;
    private PrintStream stderr;

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    @BeforeEach
    void setUpStderr() {
        stderr = System.err;
        output = new ByteArrayOutputStream();
        PrintStream testStream = new PrintStream(output, true);
        System.setErr(testStream);
        Logger.getLogger(Dashboard.class.getName())
              .setLevel(Level.OFF);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    @AfterEach
    void resetStderr() {
        System.err.close();
        System.setErr(stderr);
        Logger.getLogger(Dashboard.class.getName())
              .setLevel(Level.ALL);
    }

    @Test
    @DisplayName("log `ConstraintViolated` event")
    void acceptConstraintViolated() {
        MessageId entity = entityId();
        Dashboard.instance()
                 .on(ConstraintViolated
                             .newBuilder()
                             .setEntity(entity)
                             .vBuild());
        assertLogged(Identifier.toString(entity.getId()));
    }

    @Test
    @DisplayName("log `CannotDispatchDuplicateCommand` event")
    void acceptCannotDispatchDuplicateCommand() {
        MessageId command = MessageId
                .newBuilder()
                .setId(pack(CommandId.generate()))
                .setTypeUrl(TypeUrl.of(BbCreateProject.class).value())
                .vBuild();
        Dashboard.instance()
                 .on(CannotDispatchDuplicateCommand
                             .newBuilder()
                             .setEntity(entityId())
                             .setDuplicateCommand(command)
                             .vBuild());
        assertLogged(command.getTypeUrl());
        assertLogged(command.asCommandId().getUuid());
    }

    @Test
    @DisplayName("log `CannotDispatchDuplicateEvent` event")
    void acceptCannotDispatchDuplicateEvent() {
        MessageId event = MessageId
                .newBuilder()
                .setId(pack(EventId.newBuilder()
                                   .setValue(newUuid())
                                   .build()))
                .setTypeUrl(TypeUrl.of(BbProjectCreated.class).value())
                .vBuild();
        Dashboard.instance()
                 .on(CannotDispatchDuplicateEvent
                             .newBuilder()
                             .setEntity(entityId())
                             .setDuplicateEvent(event)
                             .vBuild());
        assertLogged(event.getTypeUrl());
        assertLogged(event.asEventId().getValue());
    }

    @Test
    @DisplayName("log `HandlerFailedUnexpectedly` event")
    void acceptHandlerFailedUnexpectedly() {
        MessageId entity = entityId();
        Error error = causeOf(new IllegalStateException("Test exception. Handler is fine."));
        Dashboard.instance()
                 .on(HandlerFailedUnexpectedly
                             .newBuilder()
                             .setEntity(entity)
                             .setError(error)
                             .vBuild());
        assertLogged(error.getMessage());
    }

    @Test
    @DisplayName("log `RoutingFailed` event")
    void acceptRoutingFailed() {
        Error error = causeOf(new IllegalStateException("Test exception. Routing is fine."));
        Dashboard.instance()
                 .on(RoutingFailed
                             .newBuilder()
                             .setError(error)
                             .vBuild());
        assertLogged(error.getMessage());
    }

    @Test
    @DisplayName("log `AggregateHistoryCorrupted` event")
    void acceptAggregateHistoryCorrupted() {
        Error error =
                fromThrowable(new IllegalStateException("Test exception. Aggregates are fine."));
        MessageId entityId = entityId();
        Dashboard.instance()
                 .on(AggregateHistoryCorrupted
                             .newBuilder()
                             .setEntity(entityId)
                             .setError(error)
                             .vBuild());
        assertLogged(Identifier.toString(entityId.getId()));
        assertLogged(error.getMessage());
    }

    private void assertLogged(String messagePart) {
        assertThat(output.toString())
                .contains(messagePart);
    }

    private static MessageId entityId() {
        return MessageId
                .newBuilder()
                .setId(pack(newUuid()))
                .setTypeUrl(TypeUrl.of(Empty.class).value())
                .vBuild();
    }
}
