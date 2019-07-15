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

package io.spine.server;

import com.google.common.base.Objects;
import io.spine.core.BoundedContextName;
import io.spine.core.BoundedContextNames;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.BoundedContextNames.newName;

/**
 * Specification of a bounded context.
 *
 * <p>The spec includes the values required to build a {@link BoundedContext}.
 */
public final class ContextSpec {

    private final BoundedContextName name;
    private final boolean multitenant;
    private final boolean system;

    private ContextSpec(BoundedContextName name, boolean multitenant, boolean system) {
        this.name = checkNotNull(name);
        this.multitenant = multitenant;
        this.system = system;
    }

    /**
     * Creates a spec of a single tenant context with the given name.
     */
    public static ContextSpec singleTenant(String name) {
        return createDomain(name, false);
    }

    /**
     * Creates a spec of a multitenant context with the given name.
     */
    public static ContextSpec multitenant(String name) {
        return createDomain(name, true);
    }

    private static ContextSpec createDomain(String name, boolean multitenant) {
        checkNotNull(name);
        BoundedContextName contextName = newName(name);
        return new ContextSpec(contextName, multitenant, false);
    }

    ContextSpec toSystem() {
        checkArgument(!isSystem());
        BoundedContextName systemName = BoundedContextNames.system(name);
        return new ContextSpec(systemName, multitenant, true);
    }

    /**
     * Obtains the context name.
     */
    public BoundedContextName name() {
        return name;
    }

    /**
     * Checks if the context is multitenant or not.
     */
    public boolean isMultitenant() {
        return multitenant;
    }

    public boolean isSystem() {
        return system;
    }

    /**
     * Returns a single-tenant version of this instance, if it is multitenant, or
     * this instance if it is single-tenant.
     */
    public ContextSpec toSingleTenant() {
        if (isMultitenant()) {
            ContextSpec result = singleTenant(name.getValue());
            return result;
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContextSpec)) {
            return false;
        }
        ContextSpec spec = (ContextSpec) o;
        return isMultitenant() == spec.isMultitenant() &&
                Objects.equal(name, spec.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, isMultitenant());
    }

    @Override
    public String toString() {
        String tenancy = multitenant
                         ? "Multitenant"
                         : "Single tenant";
        return String.format("%s context %s", tenancy, name.getValue());
    }
}
