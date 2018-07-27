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

import com.google.common.testing.EqualsTester;
import io.grpc.stub.StreamObserver;
import io.spine.core.BoundedContextName;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.server.commandbus.Given;
import io.spine.server.delivery.given.ShardedStreamTestEnv;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.transport.TransportFactory;
import io.spine.test.aggregate.ProjectId;
import io.spine.testing.Tests;
import io.spine.testing.server.model.ModelTests;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static io.spine.server.delivery.given.ShardedStreamTestEnv.anotherProjectsShardOne;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.anotherProjectsShardZero;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.projectsShardOne;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.projectsShardZero;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.streamToShardWithFactory;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.tasksShardOne;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.tasksShardZero;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alex Tymchenko
 */
@DisplayName("ShardedStream should")
class ShardedStreamTest {

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("support equality")
    void supportEquality() {
        ModelTests.clearModel();
        new EqualsTester().addEqualityGroup(projectsShardZero(), anotherProjectsShardZero())
                          .addEqualityGroup(projectsShardOne(), anotherProjectsShardOne())
                          .addEqualityGroup(tasksShardZero())
                          .addEqualityGroup(tasksShardOne())
                          .testEquals();
    }

    @Test
    @DisplayName("support `toString`")
    void supportToString() {
        CommandShardedStream<ProjectId> stream = projectsShardZero();
        ShardingKey key = stream.getKey();
        DeliveryTag<CommandEnvelope> tag = stream.getTag();
        Class<ProjectId> targetClass = ProjectId.class;
        BoundedContextName contextName = tag.getBoundedContextName();

        String stringRepresentation = stream.toString();

        assertTrue(stringRepresentation.contains(key.toString()));
        assertTrue(stringRepresentation.contains(tag.toString()));
        assertTrue(stringRepresentation.contains(targetClass.toString()));
        assertTrue(stringRepresentation.contains(contextName.toString()));
    }

    @Test
    @DisplayName("throw ISE if incoming stream is completed")
    void throwOnStreamCompleted() {
        Function<StreamObserver<ExternalMessage>, Void> onCompletedCallback =
                new Function<StreamObserver<ExternalMessage>, Void>() {
                    @Override
                    public @Nullable Void
                    apply(@Nullable StreamObserver<ExternalMessage> observer) {
                        observer.onCompleted();
                        return Tests.<Void>nullRef();
                    }
                };
        // Create a factory, which calls {@code onCompleted()} for the subscriber upon any message.
        TransportFactory factory = ShardedStreamTestEnv.customFactory(onCompletedCallback);

        CommandShardedStream<ProjectId> stream = streamToShardWithFactory(factory);
        Command createProject = Given.ACommand.createProject();
        assertThrows(IllegalStateException.class,
                     () -> stream.post(ProjectId.getDefaultInstance(),
                                       CommandEnvelope.of(createProject)));
    }

    @Test
    @DisplayName("throw ISE if incoming stream is errored")
    void throwOnStreamErrored() {

        // Create a factory, which calls {@code onError()} for the subscriber upon any message.
        Function<StreamObserver<ExternalMessage>, Void> onErrorCallback =
                new Function<StreamObserver<ExternalMessage>, Void>() {
                    @Override
                    public @Nullable Void
                    apply(@Nullable StreamObserver<ExternalMessage> observer) {
                        observer.onError(new RuntimeException("Something went wrong"));
                        return Tests.<Void>nullRef();
                    }
                };

        TransportFactory factory = ShardedStreamTestEnv.customFactory(onErrorCallback);

        CommandShardedStream<ProjectId> stream = streamToShardWithFactory(factory);
        Command createProject = Given.ACommand.createProject();
        assertThrows(IllegalStateException.class,
                     () -> stream.post(ProjectId.getDefaultInstance(),
                                       CommandEnvelope.of(createProject)));
    }
}
