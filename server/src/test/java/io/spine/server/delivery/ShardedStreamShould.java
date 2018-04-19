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
package io.spine.server.delivery;

import com.google.common.testing.EqualsTester;
import io.spine.core.BoundedContextName;
import io.spine.core.CommandEnvelope;
import io.spine.server.model.ModelTests;
import io.spine.test.aggregate.ProjectId;
import org.junit.Test;

import static io.spine.server.delivery.given.ShardedStreamTestEnv.anotherOneToProjects;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.anotherZeroToProjects;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.commandStreamToTasksOne;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.commandStreamToTasksZero;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.streamOneToProjects;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.streamZeroToProjects;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
public class ShardedStreamShould {

    @Test
    public void support_equality() {
        ModelTests.clearModel();
        new EqualsTester().addEqualityGroup(streamZeroToProjects(), anotherZeroToProjects())
                          .addEqualityGroup(streamOneToProjects(), anotherOneToProjects())
                          .addEqualityGroup(commandStreamToTasksZero())
                          .addEqualityGroup(commandStreamToTasksOne())
                          .testEquals();
    }

    @Test
    public void support_toString() {
        final CommandShardedStream<ProjectId> stream = streamZeroToProjects();
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
}
