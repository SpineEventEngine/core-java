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

package io.spine.server.groups;

import com.google.common.collect.ImmutableSet;
import io.spine.core.Subscribe;
import io.spine.server.organizations.Organization;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.StateUpdateRouting;

public final class GroupNameProjection
        extends Projection<GroupId, GroupName, GroupNameVBuilder> {

    private GroupNameProjection(GroupId id) {
        super(id);
    }

    @Subscribe(external = true)
    public void onUpdate(Organization organization) {
        getBuilder()
                .setId(id())
                .setName(organization.getName());
    }

    public static final class Repository
            extends ProjectionRepository<GroupId, GroupNameProjection, GroupName> {

        @Override
        public void onRegistered() {
            super.onRegistered();

            StateUpdateRouting<GroupId> routing = StateUpdateRouting.newInstance();
            routing.route(Organization.class, (org, ctx) -> ImmutableSet.of(
                    GroupId.newBuilder()
                           .setUuid(org.getId()
                                       .getUuid())
                           .build()));
            eventRouting().routeEntityStateUpdates(routing);
        }
    }
}
