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

package io.spine.server.tenant;

import com.google.common.collect.ImmutableList;
import io.spine.core.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.TestValues.randomString;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("SingleTenantIndex should")
class SingleTenantIndexTest {

    private final TenantIndex index = TenantIndex.singleTenant();

    @Test
    @DisplayName("not add passed IDs")
    void keep() {
        List<TenantId> before = ImmutableList.copyOf(index.getAll());
        index.keep(TenantId.newBuilder()
                           .setValue(randomString())
                           .build());
        List<TenantId> after = ImmutableList.copyOf(index.getAll());
        assertThat(after).isEqualTo(before);
    }

    @Test
    @DisplayName("return set with one element")
    void getAll() {
        List<TenantId> items = ImmutableList.copyOf(index.getAll());

        assertThat(items).containsExactly(SingleTenantIndex.tenantId());
    }
}
