/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.bus.given.stock;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.test.bus.IndexName;
import io.spine.test.bus.JowDons;
import io.spine.test.bus.PriceDropped;
import io.spine.test.bus.PriceRaised;

import static java.lang.Math.round;

public final class JowDonsIndex extends Projection<IndexName, JowDons, JowDons.Builder> {

    public static final IndexName JOW_DONS = IndexName
            .newBuilder()
            .setName("Jow Dons")
            .vBuild();

    @Subscribe
    void on(PriceRaised event) {
        ensureName();
        builder().setPoints(builder().getPoints() + round(event.getPercent()));
    }

    @Subscribe
    void on(PriceDropped event) {
        ensureName();
        builder().setPoints(builder().getPoints() - round(event.getPercent()));
    }

    private void ensureName() {
        builder().setName(id());
    }

    public static final class Repository
            extends ProjectionRepository<IndexName, JowDonsIndex, JowDons> {

        @OverridingMethodsMustInvokeSuper
        @Override
        protected void setupEventRouting(EventRouting<IndexName> routing) {
            super.setupEventRouting(routing);
            routing.replaceDefault((event, context) -> EventRoute.withId(JOW_DONS));
        }
    }
}
