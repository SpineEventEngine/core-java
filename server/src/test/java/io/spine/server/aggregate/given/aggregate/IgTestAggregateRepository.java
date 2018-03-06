/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.aggregate.given.aggregate;

import com.google.common.base.Optional;
import io.spine.core.TenantId;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.tenant.TenantAwareFunction;
import io.spine.test.aggregate.ProjectId;

import static org.junit.Assert.fail;

/**
 * @author Mykhailo Drachuk
 * @author Alexander Yevsyukov
 */
public class IgTestAggregateRepository
        extends AggregateRepository<ProjectId, IgTestAggregate> {

    /**
     * Helper method for loading an aggregate by its ID.
     */
    public IgTestAggregate loadAggregate(TenantId tenantId, ProjectId id) {
        final TenantAwareFunction<ProjectId, IgTestAggregate> load =
                new TenantAwareFunction<ProjectId, IgTestAggregate>(tenantId) {
                    @Override
                    public IgTestAggregate apply(ProjectId input) {
                        final Optional<IgTestAggregate> optional = find(input);
                        if (!optional.isPresent()) {
                            fail("Aggregate not found.");
                        }
                        return optional.get();
                    }
                };
        return load.execute(id);
    }
}
