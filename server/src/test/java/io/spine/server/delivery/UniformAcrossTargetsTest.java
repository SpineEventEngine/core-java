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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.delivery.UniformAcrossTargets.forNumber;
import static io.spine.server.delivery.UniformAcrossTargets.singleShard;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Alex Tymchenko
 */
@DisplayName("UniformAcrossTargets should")
class UniformAcrossTargetsTest {

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("support equality")
    void supportEquality() {
        new EqualsTester().addEqualityGroup(singleShard(), singleShard(), forNumber(1))
                          .addEqualityGroup(forNumber(3), forNumber(3))
                          .addEqualityGroup(forNumber(42), forNumber(42))
                          .testEquals();
    }

    @Test
    @DisplayName("not allow negative number of shards")
    void notAllowNegativeShardCount() {
        assertThrows(IllegalArgumentException.class, () -> forNumber(-1));
    }

    @Test
    @DisplayName("not allow zero number of shards")
    void notAllowZeroShardCount() {
        assertThrows(IllegalArgumentException.class, () -> forNumber(0));
    }
}
