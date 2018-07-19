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

package io.spine.server.aggregate;

import com.google.common.testing.EqualsTester;
import io.spine.testing.Tests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dmytro Grankin
 */
@DisplayName("AggregateReadRequest should")
class AggregateReadRequestTest {

    private static final String ID = "ID";
    private static final int BATCH_SIZE = 10;

    @Test
    @DisplayName("not accept null ID")
    void notAcceptNullID() {
        assertThrows(NullPointerException.class,
                     () -> new AggregateReadRequest<>(Tests.<String>nullRef(), BATCH_SIZE));
    }

    @Test
    @DisplayName("not accept non-positive batch size")
    void notAcceptNonPositiveBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> new AggregateReadRequest<>(ID, 0));
    }

    @Test
    @DisplayName("consider request with same ID equal")
    void considerRequestWithSameIdEqual() {
        AggregateReadRequest<String> first = new AggregateReadRequest<>(ID, BATCH_SIZE);
        int differentBatch = first.getBatchSize() * 2;
        AggregateReadRequest<String> second = new AggregateReadRequest<>(ID,
                                                                         differentBatch);
        new EqualsTester().addEqualityGroup(first, second)
                          .testEquals();
    }
}
