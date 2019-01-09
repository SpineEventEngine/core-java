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

package io.spine.client.given;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.client.Filters;
import io.spine.client.FiltersVBuilder;
import io.spine.client.IdFilter;
import io.spine.client.IdFilterVBuilder;
import io.spine.test.queries.TaskId;
import io.spine.test.queries.TaskIdVBuilder;

import java.util.List;

import static io.spine.base.Identifier.newUuid;
import static io.spine.protobuf.AnyPacker.pack;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class TargetsTestEnv {

    /** Prevents instantiation of this test environment class. */
    private TargetsTestEnv() {
    }

    public static Filters filtersForIds(Message... ids) {
        List<Message> idsList = asList(ids);
        List<Any> entityIds = idsList.stream()
                                     .map(TargetsTestEnv::newId)
                                     .collect(toList());
        IdFilter idFilter = IdFilterVBuilder.newBuilder()
                                            .addAllIds(entityIds)
                                            .build();
        return FiltersVBuilder.newBuilder()
                              .setIdFilter(idFilter)
                              .build();
    }

    private static Any newId(Message item) {
        return pack(item);
    }

    public static TaskId newTaskId() {
        return TaskIdVBuilder.newBuilder()
                             .setValue(newUuid())
                             .build();
    }
}
