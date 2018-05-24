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

import com.google.common.base.Function;
import com.google.common.testing.EqualsTester;
import io.grpc.stub.StreamObserver;
import io.spine.core.BoundedContextName;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.server.commandbus.Given;
import io.spine.server.delivery.given.ShardedStreamTestEnv;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.model.ModelTests;
import io.spine.server.transport.TransportFactory;
import io.spine.test.Tests;
import io.spine.test.aggregate.ProjectId;
import org.junit.Test;

import org.checkerframework.checker.nullness.qual.Nullable;

import static io.spine.server.delivery.given.ShardedStreamTestEnv.anotherProjectsShardOne;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.anotherProjectsShardZero;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.projectsShardOne;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.projectsShardZero;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.streamToShardWithFactory;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.tasksShardOne;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.tasksShardZero;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
public class ShardedStreamShould {

    @Test
    public void support_equality() {
        ModelTests.clearModel();
        new EqualsTester().addEqualityGroup(projectsShardZero(), anotherProjectsShardZero())
                          .addEqualityGroup(projectsShardOne(), anotherProjectsShardOne())
                          .addEqualityGroup(tasksShardZero())
                          .addEqualityGroup(tasksShardOne())
                          .testEquals();
    }

    @Test
    public void support_toString() {
        final CommandShardedStream<ProjectId> stream = projectsShardZero();
        final ShardingKey key = stream.getKey();
        final DeliveryTag<CommandEnvelope> tag = stream.getTag();
        final Class<ProjectId> targetClass = ProjectId.class;
        final BoundedContextName contextName = tag.getBoundedContextName();

        final String stringRepresentation = stream.toString();

        assertTrue(stringRepresentation.contains(key.toString()));
        assertTrue(stringRepresentation.contains(tag.toString()));
        assertTrue(stringRepresentation.contains(targetClass.toString()));
        assertTrue(stringRepresentation.contains(contextName.toString()));
    }

    @Test(expected = IllegalStateException.class)
    public void throw_ISE_if_incoming_stream_is_completed() {
        final Function<StreamObserver<ExternalMessage>, Void> onCompletedCallback =
                new Function<StreamObserver<ExternalMessage>, Void>() {
                    @Nullable
                    @Override
                    public Void apply(@Nullable StreamObserver<ExternalMessage> observer) {
                        observer.onCompleted();
                        return Tests.<Void>nullRef();
                    }
                };
        // Create a factory, which calls {@code onCompleted()} for the subscriber upon any message.
        final TransportFactory factory = ShardedStreamTestEnv.customFactory(onCompletedCallback);

        final CommandShardedStream<ProjectId> stream = streamToShardWithFactory(factory);
        final Command createProject = Given.ACommand.createProject();
        stream.post(ProjectId.getDefaultInstance(), CommandEnvelope.of(createProject));
    }

    @Test(expected = IllegalStateException.class)
    public void throw_ISE_if_incoming_stream_is_errored() {

        // Create a factory, which calls {@code onError()} for the subscriber upon any message.
        final Function<StreamObserver<ExternalMessage>, Void> onErrorCallback =
                new Function<StreamObserver<ExternalMessage>, Void>() {
                    @Nullable
                    @Override
                    public Void apply(@Nullable StreamObserver<ExternalMessage> observer) {
                        observer.onError(new RuntimeException("Something went wrong"));
                        return Tests.<Void>nullRef();
                    }
                };

        final TransportFactory factory = ShardedStreamTestEnv.customFactory(onErrorCallback);

        final CommandShardedStream<ProjectId> stream = streamToShardWithFactory(factory);
        final Command createProject = Given.ACommand.createProject();
        stream.post(ProjectId.getDefaultInstance(), CommandEnvelope.of(createProject));
    }
}
