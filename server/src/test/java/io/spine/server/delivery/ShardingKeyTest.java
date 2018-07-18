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
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.delivery.given.ShardedStreamTestEnv.anotherProjectsShardZero;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.projectsShardOne;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.projectsShardZero;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.tasksShardOne;
import static io.spine.server.delivery.given.ShardedStreamTestEnv.tasksShardZero;

/**
 * @author Alex Tymchenko
 */
@DisplayName("ShardingKey should")
class ShardingKeyTest {

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("support equality")
    void supportEquality() {
        ModelTests.clearModel();

        ShardingKey projectsZero = projectsShardZero().getKey();
        ShardingKey theSameProjectsZero = anotherProjectsShardZero().getKey();
        ShardingKey projectsOne = projectsShardOne().getKey();
        ShardingKey tasksZeroKey = tasksShardZero().getKey();
        ShardingKey tasksOneKey = tasksShardOne().getKey();

        new EqualsTester().addEqualityGroup(projectsZero, theSameProjectsZero)
                          .addEqualityGroup(projectsOne)
                          .addEqualityGroup(tasksZeroKey)
                          .addEqualityGroup(tasksOneKey)
                          .testEquals();
    }
}
