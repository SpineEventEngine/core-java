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

package io.spine.testing.server.blackbox.verify.state;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.core.TenantId;
import io.spine.testing.server.blackbox.BlackBoxOutput;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;

/**
 * Queries entities by the state type for a tenant and verifies that
 * entity states match the {@linkplain #expected specified} ones.
 */
@VisibleForTesting
class VerifyByTypeForTenant<T extends Message> extends VerifyState {

    private final Iterable<T> expected;
    private final Class<T> entityType;
    private final TenantId tenantId;

    VerifyByTypeForTenant(Iterable<T> expected, Class<T> entityType, TenantId tenantId) {
        this.expected = ImmutableList.copyOf(expected);
        this.entityType = checkNotNull(entityType);
        this.tenantId = checkNotNull(tenantId);
    }

    @Override
    public void verify(BlackBoxOutput output) {
        List<T> actual = output.entities(entityType, tenantId);
        assertThat(actual).containsExactlyElementsIn(expected);
    }
}
