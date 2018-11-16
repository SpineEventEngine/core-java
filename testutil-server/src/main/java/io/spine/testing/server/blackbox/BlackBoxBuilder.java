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

package io.spine.testing.server.blackbox;

import io.spine.core.TenantId;
import io.spine.server.event.Enricher;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Identifier.newUuid;

/**
 * The builder of a {@link BlackBoxBoundedContext}.
 */
public class BlackBoxBuilder {

    private TenantId tenantId;
    private Enricher enricher;

    /** Prevents instantiation of this class outside of the package. */
    BlackBoxBuilder() {
    }

    /**
     * Sets the default tenant ID for the bounded context.
     *
     * <p>It is possible to perform operations with a different tenant ID by supplying
     * {@linkplain io.spine.core.Command commands} and {@linkplain io.spine.core.Event events}
     * with the different tenant ID.
     *
     * @param tenant
     *         the {@link TenantId} to use when producing commands and events
     */
    public BlackBoxBuilder withTenant(TenantId tenant) {
        this.tenantId = checkNotNull(tenant);
        return this;
    }

    /**
     * Sets the enricher for the bounded context.
     *
     * @param enricher the enricher to use
     */
    public BlackBoxBuilder withEnricher(Enricher enricher) {
        this.enricher = checkNotNull(enricher);
        return this;
    }

    TenantId buildTenant() {
        if (tenantId == null) {
            tenantId = newTenantId();
        }
        return tenantId;
    }

    Enricher buildEnricher() {
        if (enricher == null) {
            enricher = Enricher.newBuilder()
                               .build();
        }
        return enricher;
    }

    public BlackBoxBoundedContext build() {
        return new BlackBoxBoundedContext(this);
    }

    /**
     * Creates a new {@link TenantId Tenant ID} with a random UUID value convenient
     * for test purposes.
     */
    private static TenantId newTenantId() {
        return TenantId.newBuilder()
                       .setValue(newUuid())
                       .build();
    }
}
