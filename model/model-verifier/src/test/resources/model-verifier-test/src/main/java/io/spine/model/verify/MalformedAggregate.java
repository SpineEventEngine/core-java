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

package io.spine.model.verify;

import com.google.protobuf.Any;
import com.google.protobuf.UInt64Value;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.command.Assign;
import io.spine.server.test.shared.EmptyAggregate;
import io.spine.server.test.shared.EmptyAggregateVBuilder;

import java.util.Collections;
import java.util.List;

/**
 * An Aggregate with n invalid command handler method.
 *
 * <p>{@link #handle()} method has no arguments and is marked with {@link Assign}, which makes it
 * an invalid command handler method.
 */
public class MalformedAggregate extends Aggregate<String, EmptyAggregate, EmptyAggregateVBuilder> {

    protected MalformedAggregate(String id) {
        super(id);
    }

    @Assign
    public List<UInt64Value> handle() {
        return Collections.emptyList();
    }
}
