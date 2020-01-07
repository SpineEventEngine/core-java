/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.delivery.given;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.test.delivery.ConsecutiveNumberView;
import io.spine.test.delivery.NegativeNumberEmitted;
import io.spine.test.delivery.PositiveNumberEmitted;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static io.spine.util.Exceptions.newIllegalArgumentException;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.lang.Math.abs;

/**
 * @author Alex Tymchenko
 */
public class ConsecutiveProjection
        extends Projection<String, ConsecutiveNumberView, ConsecutiveNumberView.Builder> {

    private static UsageMode mode = UsageMode.POSITIVES_ONLY;

    public static void useNegatives() {
        mode = UsageMode.NEGATIVES_ONLY;
    }

    public static void usePositives() {
        mode = UsageMode.POSITIVES_ONLY;
    }

    @Subscribe
    void on(PositiveNumberEmitted event) {
        updateState(event.getId(), event.getValue());
    }

    @Subscribe
    void on (NegativeNumberEmitted event) {
        updateState(event.getId(), event.getValue());
    }

    private void updateState(String id, int newValue) {
        builder().setId(id);

        System.out.println(String.format("[%s] got `%d`.", id, newValue));
        mode.validate(newValue);

        int lastValue = state().getLastValue();
        if (abs(newValue) - abs(lastValue) != 1) {
            throw newIllegalStateException(
                    "ConsecutiveNumberProjection with ID `%s` got wrong value. " +
                            "Current value is %d, but got `%d`.",
                    id, lastValue, newValue);
        }

        builder().setLastValue(newValue);
    }

    public enum UsageMode {
        POSITIVES_ONLY((value) -> value > 0),
        NEGATIVES_ONLY((value) -> value < 0);

        private final Function<Integer, Boolean> validator;

        UsageMode(Function<Integer, Boolean> validator) {
            this.validator = validator;
        }

        void validate(int incoming) {
            Boolean valid = validator.apply(incoming);
            if (!valid) {
                throw newIllegalArgumentException(
                        "The mode `%s` does not accept the supplied `%d` value.",
                        this.toString(), incoming);
            }
        }
    }

    public static final class  Repository
            extends ProjectionRepository<String, ConsecutiveProjection, ConsecutiveNumberView> {

        private final Set<String> excludedTargets = new HashSet<>();

        @OverridingMethodsMustInvokeSuper
        @Override
        protected void setupEventRouting(EventRouting<String> routing) {
            super.setupEventRouting(routing);
            routing.route(PositiveNumberEmitted.class,
                          (EventRoute<String, PositiveNumberEmitted>) (message, context) ->
                                  mode == UsageMode.POSITIVES_ONLY
                                  ? withoutExcluded(ImmutableSet.of(message.getId()))
                                  : ImmutableSet.of())
                   .route(NegativeNumberEmitted.class,
                          (EventRoute<String, NegativeNumberEmitted>) (message, context) ->
                                  mode == UsageMode.NEGATIVES_ONLY
                                  ? withoutExcluded(ImmutableSet.of(message.getId()))
                                  : ImmutableSet.of());
        }

        private Set<String> withoutExcluded(Set<String> original) {
            return Sets.difference(original, excludedTargets);
        }

        public void excludeTarget(String targetId) {
            excludedTargets.add(targetId);
        }
    }
}
