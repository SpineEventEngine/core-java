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

package io.spine.server.route.given.sur;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.server.route.StateUpdateRouting;
import io.spine.server.route.given.sur.command.ArtistMood;
import io.spine.server.route.given.sur.command.ArtistName;
import io.spine.server.route.given.sur.event.ArticlePublished;

import java.util.Optional;

import static io.spine.server.route.given.sur.Surrealism.allTheseGentlemen;
import static io.spine.server.route.given.sur.Surrealism.opponentOf;

public class ArtistMoodRepo
        extends ProjectionRepository<ArtistName, ArtistMoodProjection, ArtistMood> {

    @OverridingMethodsMustInvokeSuper
    @Override
    protected void setupEventRouting(EventRouting<ArtistName> routing) {
        super.setupEventRouting(routing);
        routing.route(ArticlePublished.class,
                      (event, context) -> allTheseGentlemen());
    }

    @Override
    protected void setupStateRouting(StateUpdateRouting<ArtistName> routing) {
        super.setupStateRouting(routing);
        routing.route(ArtistMood.class, (mood, context) -> {
            Optional<ArtistName> opponent = opponentOf(mood.getName());
            return opponent.map(EventRoute::withId)
                           .orElseGet(EventRoute::noTargets);
        });
    }

}
