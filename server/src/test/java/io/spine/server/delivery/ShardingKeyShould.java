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
import io.spine.server.model.ModelTests;
import org.junit.Test;

import static io.spine.server.delivery.given.ShardedStreamTestEnv.anotherZeroToProjects;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.commandStreamToTasksOne;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.commandStreamToTasksZero;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.streamOneToProjects;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.streamZeroToProjects;

/**
 * @author Alex Tymchenko
 */
public class ShardingKeyShould {

    @Test
    public void support_equality() {
        ModelTests.clearModel();

        final ShardingKey projectsZero = streamZeroToProjects().getKey();
        final ShardingKey theSameProjectsZero = anotherZeroToProjects().getKey();
        final ShardingKey projectsOne = streamOneToProjects().getKey();
        final ShardingKey tasksZeroKey = commandStreamToTasksZero().getKey();
        final ShardingKey tasksOneKey = commandStreamToTasksOne().getKey();

        new EqualsTester().addEqualityGroup(projectsZero, theSameProjectsZero)
                          .addEqualityGroup(projectsOne)
                          .addEqualityGroup(tasksZeroKey)
                          .addEqualityGroup(tasksOneKey)
                          .testEquals();
    }
}
