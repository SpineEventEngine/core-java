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

package io.spine.system.server;

import io.spine.client.ActorRequestFactory;
import io.spine.client.CommandFactory;
import io.spine.core.TenantId;
import io.spine.server.tenant.TenantFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.system.server.DefaultSystemWriteSide.SYSTEM_USER;

/**
 * Creates a command factory for producing commands under the context of specified tenant.
 */
final class SystemCommandFactory {

    private static final ActorRequestFactory SINGLE_TENANT =
            newFactoryFor(TenantId.getDefaultInstance());

    /** Prevents instantiation of this utility class. */
    private SystemCommandFactory() {
    }

    /**
     * Obtains a {@code CommandFactory} for creating system commands.
     *
     * @param multitenant
     *        pass {@code true} if the System Context works in the multi-tenant mode,
     *        {@code false} otherwise
     */
    static CommandFactory newInstance(boolean multitenant) {
        ActorRequestFactory requestFactory = requestFactory(multitenant);
        return requestFactory.command();
    }

    static ActorRequestFactory requestFactory(boolean multitenant) {
        return multitenant
               ? newForCurrentTenant()
               : SINGLE_TENANT;
    }

    private static ActorRequestFactory newForCurrentTenant() {
        TenantFunction<ActorRequestFactory> createFactory =
                new TenantFunction<ActorRequestFactory>(true) {
                    @Override
                    public ActorRequestFactory apply(@Nullable TenantId tenantId) {
                        checkNotNull(tenantId);
                        return newFactoryFor(tenantId);
                    }
                };
        ActorRequestFactory result = createFactory.execute();
        checkNotNull(result);
        return result;
    }

    private static ActorRequestFactory newFactoryFor(TenantId tenantId) {
        return ActorRequestFactory.newBuilder()
                                  .setActor(SYSTEM_USER)
                                  .setTenantId(tenantId)
                                  .build();
    }
}
