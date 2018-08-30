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

package io.spine.system.server;

import io.spine.core.Subscribe;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.projection.Projection;

/**
 * @author Dmytro Dashenkov
 */
public class MirrorProjection extends Projection<MirrorId, Mirror, MirrorVBuilder> {

    private MirrorProjection(MirrorId id) {
        super(id);
    }

    @Subscribe
    public void on(EntityStateChanged event) {
        getBuilder().setId(getId())
                    .setState(event.getNewState());
    }

    @Subscribe
    public void on(@SuppressWarnings("unused")
                   EntityArchived event) {
        LifecycleFlags flags = getBuilder().getLifecycle()
                                           .toBuilder()
                                           .setArchived(true)
                                           .build();
        getBuilder().setId(getId())
                    .setLifecycle(flags);
        setArchived(true);
    }

    @Subscribe
    public void on(@SuppressWarnings("unused")
                   EntityDeleted event) {
        LifecycleFlags flags = getBuilder().getLifecycle()
                                           .toBuilder()
                                           .setDeleted(true)
                                           .build();
        getBuilder().setId(getId())
                    .setLifecycle(flags);
        setDeleted(true);
    }

    @Subscribe
    public void on(@SuppressWarnings("unused")
                   EntityExtractedFromArchive event) {
        LifecycleFlags flags = getBuilder().getLifecycle()
                                           .toBuilder()
                                           .setArchived(false)
                                           .build();
        getBuilder().setId(getId())
                    .setLifecycle(flags);
        setArchived(false);
    }

    @Subscribe
    public void on(@SuppressWarnings("unused")
                   EntityRestored event) {
        LifecycleFlags flags = getBuilder().getLifecycle()
                                           .toBuilder()
                                           .setDeleted(false)
                                           .build();
        getBuilder().setId(getId())
                    .setLifecycle(flags);
        setDeleted(false);
    }
}
