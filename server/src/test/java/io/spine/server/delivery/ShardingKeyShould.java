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

import static io.spine.server.delivery.given.ShardedStreamTestEnv.anotherProjectsShardZero;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.projectsShardOne;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.projectsShardZero;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.tasksShardOne;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.tasksShardZero;

/**
 * @author Alex Tymchenko
 */
public class ShardingKeyShould {

    @Test
    public void support_equality() {
        ModelTests.clearModel();

        final ShardingKey projectsZero = projectsShardZero().getKey();
        final ShardingKey theSameProjectsZero = anotherProjectsShardZero().getKey();
        final ShardingKey projectsOne = projectsShardOne().getKey();
        final ShardingKey tasksZeroKey = tasksShardZero().getKey();
        final ShardingKey tasksOneKey = tasksShardOne().getKey();

        new EqualsTester().addEqualityGroup(projectsZero, theSameProjectsZero)
                          .addEqualityGroup(projectsOne)
                          .addEqualityGroup(tasksZeroKey)
                          .addEqualityGroup(tasksOneKey)
                          .testEquals();
    }
}
