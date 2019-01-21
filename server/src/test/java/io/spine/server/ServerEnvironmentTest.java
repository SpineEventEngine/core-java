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

package io.spine.server;

import io.spine.server.sharding.ShardingStrategy;
import io.spine.server.sharding.UniformAcrossAllShards;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("ServerEnvironment utility should")
class ServerEnvironmentTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(ServerEnvironment.class);
    }

    @Test
    @DisplayName("tell when not running under AppEngine")
    void tellIfNotInAppEngine() {
        // Tests are not run by AppEngine by default.
        assertFalse(ServerEnvironment.getInstance().isAppEngine());
    }

    @Test
    @DisplayName("obtain AppEngine version as optional string")
    void getAppEngineVersion() {
        // By default we're not running under AppEngine.
        assertFalse(ServerEnvironment.getInstance().appEngineVersion()
                                     .isPresent());
    }

    @Test
    @DisplayName("return single-shard sharding strategy by default")
    void returnSingleShardStrategyDefault() {
        assertEquals(1,
                     ServerEnvironment.getInstance().getShardingStrategy().getShardCount());
    }

    @Test
    @DisplayName("allow to customize sharding strategy")
    void allowToCustomizeShardingStrategy() {
        ShardingStrategy strategy = UniformAcrossAllShards.forNumber(42);
        ServerEnvironment environment = ServerEnvironment.getInstance();
        ShardingStrategy defaultValue = environment.getShardingStrategy();
        environment.setShardingStrategy(strategy);
        assertEquals(strategy, environment.getShardingStrategy());

        // Restore the default value.
        environment.setShardingStrategy(defaultValue);
    }
}
