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

package io.spine.server.entity.model;

import java.util.Set;

/**
 * A class which can {@linkplain io.spine.core.Subscribe subscribe} to updates of entity states.
 *
 * <p>A class can declare methods to receive updated states of entities from the same Bounded
 * Context (“domestic” states), or states originated in another Bounded Context (“external” states).
 */
public interface StateSubscribingClass {

    /**
     * Obtains domestic entity states to which the class is subscribed.
     */
    Set<StateClass> domesticStates();

    /**
     * Obtains external entity states to which the class is subscribed.
     */
    Set<StateClass> externalStates();

    /**
     * Verifies if this class is {@linkplain io.spine.core.Subscribe subscribed} to updates of
     * entity states either domestic or external.
     */
    default boolean subscribesToStates() {
        boolean dispatchesDomestic = !domesticStates().isEmpty();
        boolean dispatchesExternal = !externalStates().isEmpty();
        return dispatchesDomestic || dispatchesExternal;
    }
}
