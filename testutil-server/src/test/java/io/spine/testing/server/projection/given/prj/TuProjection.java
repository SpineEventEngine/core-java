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

package io.spine.testing.server.projection.given.prj;

import com.google.protobuf.StringValue;
import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.testing.server.entity.given.Given;
import io.spine.validate.StringValueVBuilder;

/**
 * A dummy projection that is subscribed to a {@code StringValue} event.
 */
public final class TuProjection
        extends Projection<Long, StringValue, StringValueVBuilder> {

    public static final long ID = 1L;

    TuProjection(Long id) {
        super(id);
    }

    public static TuProjection newInstance() {
        TuProjection result =
                Given.projectionOfClass(TuProjection.class)
                     .withId(ID)
                     .withVersion(64)
                     .build();
        return result;
    }

    @Subscribe
    void on(StringValue command) {
        getBuilder().setValue(command.getValue());
    }
}
