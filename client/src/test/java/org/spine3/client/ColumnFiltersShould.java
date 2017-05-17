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

package org.spine3.client;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Timestamp;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.spine3.client.ColumnFilter.Operator.EQUAL;
import static org.spine3.client.ColumnFilters.eq;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.time.Time.getCurrentTime;

/**
 * @author Dmytro Dashenkov
 */
public class ColumnFiltersShould {

    @Test
    public void not_accept_nulls() {
        new NullPointerTester()
                .setDefault(Timestamp.class, Timestamp.getDefaultInstance())
                .testAllPublicStaticMethods(ColumnFilters.class);
    }

    @Test
    public void create_EQUALS_instances() {
        final String name = "preciseColumn";
        final Timestamp value = getCurrentTime();

        final ColumnFilter param = eq(name, value);

        assertEquals(name, param.getColumnName());
        assertEquals(pack(value), param.getValue());
        assertEquals(EQUAL, param.getOperator());
    }
}
