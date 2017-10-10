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

package io.spine.server.storage;

import com.google.common.testing.EqualsTester;
import com.google.protobuf.FieldMask;
import io.spine.test.Tests;
import org.junit.Test;

/**
 * @author Dmytro Grankin
 */
public class RecordReadRequestShould {

    private static final String ID = "ID";
    private static final FieldMask FIELD_MASK = FieldMask.getDefaultInstance();

    @Test(expected = NullPointerException.class)
    public void not_accept_null_ID() {
        RecordReadRequest.of(Tests.<String>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_field_mask() {
        RecordReadRequest.of(ID, Tests.<FieldMask>nullRef());
    }

    @Test
    public void consider_request_with_same_id_and_field_mask_equal() {
        final RecordReadRequest<String> first = RecordReadRequest.of(ID, FIELD_MASK);
        final RecordReadRequest<String> second = RecordReadRequest.of(ID, FIELD_MASK);
        new EqualsTester().addEqualityGroup(first, second)
                          .testEquals();
    }
}
