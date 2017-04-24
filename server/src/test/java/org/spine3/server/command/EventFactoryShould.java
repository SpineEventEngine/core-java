/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.command;

import com.google.protobuf.Empty;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.client.ActorRequestFactory;
import org.spine3.test.TestActorRequestFactory;

import static org.spine3.test.Tests.newUuidValue;

/**
 * @author Alexander Yevsyukov
 */
public class EventFactoryShould {

    private final ActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    @Test(expected = NullPointerException.class)
    public void require_producer_id_in_builder() {
        final CommandContext ctx = requestFactory.command()
                                                 .create(Empty.getDefaultInstance())
                                                 .getContext();
        EventFactory.newBuilder()
                    .setCommandContext(ctx)
                    .build();
    }

    @Test(expected = NullPointerException.class)
    public void require_non_null_command_context_in_builder() {
        EventFactory.newBuilder()
                    .setProducerId(newUuidValue())
                    .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void require_non_default_command_context_in_builder() {
        EventFactory.newBuilder()
                    .setProducerId(newUuidValue())
                    .setCommandContext(CommandContext.getDefaultInstance())
                    .build();
    }
}
