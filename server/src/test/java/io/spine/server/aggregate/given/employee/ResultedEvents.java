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

package io.spine.server.aggregate.given.employee;

import com.google.common.collect.ImmutableList;
import io.spine.core.Event;

import java.util.List;

/**
 * Resulted events of a {@code Command} dispatching.
 *
 * <p>Consists of events that were:
 *
 * <ul>
 *     <li>stored to {@code AggregateStorage};</li>
 *     <li>posted to {@code EventBus}.</li>
 * </ul>
 *
 * A healthy {@code Aggregate} usually stores and posts the same set of events.
 * Thus, both lists should be equal.
 */
public final class ResultedEvents {

    private final ImmutableList<Event> stored;
    private final ImmutableList<Event> posted;

    public ResultedEvents(List<Event> stored, List<Event> posted) {
        this.stored = ImmutableList.copyOf(stored);
        this.posted = ImmutableList.copyOf(posted);
    }

    public List<Event> stored() {
        return stored;
    }

    public List<Event> posted() {
        return posted;
    }
}
