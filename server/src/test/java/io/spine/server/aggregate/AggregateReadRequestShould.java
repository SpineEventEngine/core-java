/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import io.spine.test.Tests;
import org.junit.Test;

public class AggregateReadRequestShould {

    private static final String ID = "id for an aggregate request";
    private static final int SNAPSHOT_TRIGGER = 10;

    @Test(expected = NullPointerException.class)
    public void not_accept_null_ID() {
        new AggregateReadRequest<>(Tests.<String>nullRef(), SNAPSHOT_TRIGGER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void not_accept_non_positive_snapshot_trigger() {
        new AggregateReadRequest<>(ID, 0);
    }

    @Test
    public void consider_request_with_same_ID_equal() {
        final AggregateReadRequest<String> first = new AggregateReadRequest<>(ID, SNAPSHOT_TRIGGER);
        final int differentTrigger = first.getSnapshotTrigger() * 2;
        final AggregateReadRequest<String> second = new AggregateReadRequest<>(ID,
                                                                               differentTrigger);
        new EqualsTester().addEqualityGroup(first, second)
                          .testEquals();
    }
}
