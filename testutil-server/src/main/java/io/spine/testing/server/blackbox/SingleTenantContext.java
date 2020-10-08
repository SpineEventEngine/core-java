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

package io.spine.testing.server.blackbox;

import com.google.common.collect.ImmutableList;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.server.BoundedContextBuilder;
import io.spine.testing.client.TestActorRequestFactory;

import static com.google.protobuf.TextFormat.shortDebugString;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Test fixture for single-tenant Bounded Contexts.
 */
final class SingleTenantContext extends BlackBox {

    SingleTenantContext(BoundedContextBuilder b) {
        super(b);
    }

    @Override
    ImmutableList<Command> select(CommandCollector collector) {
        return collector.all();
    }

    @Override
    ImmutableList<Event> select(EventCollector collector) {
        return collector.all();
    }

    @Override
    public BlackBox withTenant(TenantId tenant) {
        throw newIllegalStateException(
                "The context `%s` is single-tenant and" +
                        " cannot operate with the passed tenant ID `%s`.",
                name().value(), shortDebugString(tenant)
        );
    }

    @Override
    TestActorRequestFactory requestFactory() {
        return actor().requests();
    }
}
