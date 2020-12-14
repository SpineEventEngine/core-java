/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.storage.memory;

import com.google.common.collect.ImmutableList;
import io.spine.server.delivery.CatchUp;
import io.spine.server.delivery.CatchUpId;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.synchronizedMap;

/**
 * The catch-up processes for for a tenant.
 */
final class TenantCatchUpRecords implements TenantStorage<CatchUpId, CatchUp> {

    private final Map<CatchUpId, CatchUp> records = synchronizedMap(new HashMap<>());

    @Override
    public Iterator<CatchUpId> index() {
        return records.keySet()
                      .iterator();
    }

    @Override
    public Optional<CatchUp> get(CatchUpId id) {
        return Optional.ofNullable(records.get(id));
    }

    @Override
    public void put(CatchUpId id, CatchUp record) {
        records.put(id, record);
    }

    @Override
    public boolean isEmpty() {
        return records.isEmpty();
    }

    ImmutableList<CatchUp> readAll() {
        return ImmutableList.copyOf(records.values());
    }
}
