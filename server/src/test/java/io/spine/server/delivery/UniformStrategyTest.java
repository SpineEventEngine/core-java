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

package io.spine.server.delivery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`UniformAcrossAllShards should")
public class UniformStrategyTest {

    @Test
    @DisplayName("return single shard strategy")
    public void singleShard() {
        DeliveryStrategy strategy = UniformAcrossAllShards.singleShard();
        assertThat(strategy.getShardCount())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("allow to create a strategy with the given number of shards")
    public void customShardNumber() {
        int shards = 42;
        DeliveryStrategy strategy = UniformAcrossAllShards.forNumber(shards);
        assertThat(strategy.getShardCount())
                .isEqualTo(shards);
    }

    @Test
    @DisplayName("not accept a negative shard number")
    public void negativeNumberOfShards() {
        assertThrows(IllegalArgumentException.class,
                     () -> UniformAcrossAllShards.forNumber(-7));
    }

    @Test
    @DisplayName("not accept a zero number of shards")
    public void zeroShards() {
        assertThrows(IllegalArgumentException.class,
                     () -> UniformAcrossAllShards.forNumber(0));
    }
}
