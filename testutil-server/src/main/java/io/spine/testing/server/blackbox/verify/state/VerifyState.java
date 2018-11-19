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
import com.google.protobuf.Message;
import io.spine.core.TenantId;
import io.spine.testing.server.blackbox.BlackBoxOutput;

import java.util.function.Function;

import static java.util.Collections.singletonList;

/**
 * Verifies the states of entities currently present in a bounded context.
 */
@VisibleForTesting
public abstract class VerifyState {

    /**
     * Verifies the entity states.
     *
     * @param output
     *         the output of a black box bounded context
     */
    public abstract void verify(BlackBoxOutput output);

    /**
     * The shortcut of {@link #exactly(Class, Iterable)} to verify that
     * only a single entity is present in the storage and its state matches the expected.
     */
    public static <T extends Message> VerifyState exactlyOne(T expected) {
        @SuppressWarnings("unchecked" /* The cast is totally safe. */)
        Class<T> messageClass = (Class<T>) expected.getClass();
        return exactly(messageClass, singletonList(expected));
    }

    /**
     * Obtains a verifier which checks that the system contains exactly the passed entity states.
     *
     * @param <T>
     *         the type of the entity state
     * @param tenantId
     *         the tenant ID of queried storage
     * @param entityType
     *         the type of the entity to query
     * @param expected
     *         the expected entity states
     * @return new instance of {@code VerifyState}
     */
    public static <T extends Message> VerifyState exactly(TenantId tenantId,
                                                          Class<T> entityType,
                                                          Iterable<T> expected) {
        return new VerifyByTypeForTenant<>(expected, entityType, tenantId);
    }

    /**
     * Obtains a verifier which checks that the system contains exactly the passed entity states.
     *
     * @param <T>
     *         the type of the entity state
     * @param entityType
     *         the type of the entity to query
     * @param expected
     *         the expected entity states
     * @return new instance of {@code VerifyState}
     */
    public static <T extends Message> VerifyState exactly(Class<T> entityType,
                                                          Iterable<T> expected) {
        return new VerifyByType<>(expected, entityType);
    }

    /**
     * Provides a {@link VerifyState} based on a {@link TenantId}.
     *
     * <p>Use the interface when a tenant ID for {@link VerifyState} should be specified
     * by a {@link io.spine.testing.server.blackbox.BlackBoxBoundedContext}.
     *
     * <p>If a user wants to specify a tenant ID on its own,
     * {@link VerifyState} should be used directly.
     */
    public interface VerifyStateByTenant extends Function<TenantId, VerifyState> {
    }
}
