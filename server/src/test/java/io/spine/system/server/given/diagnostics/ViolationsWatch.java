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

package io.spine.system.server.given.diagnostics;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.ByField;
import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRouting;
import io.spine.system.server.ConstraintViolated;
import io.spine.system.server.test.InvalidText;
import io.spine.system.server.test.WatchId;
import io.spine.validate.ConstraintViolation;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.route.EventRoute.withId;

public final class ViolationsWatch extends Projection<WatchId, InvalidText, InvalidText.Builder> {

    public static final WatchId DEFAULT = WatchId.generate();

    @Subscribe(
            filter = @ByField(
                    path = "last_message.message_type_url",
                    value = "type.spine.io/spine.system.server.test.TextValidated"
            )
    )
    void on(ConstraintViolated event) {
        List<ConstraintViolation> violations = event.getViolationList();
        checkArgument(violations.size() == 1);
        ConstraintViolation violation = violations.get(0);
        Message value = unpack(violation.getFieldValue());
        checkArgument(value instanceof StringValue);
        @SuppressWarnings("OverlyStrongTypeCast") // OrBuilder
        String stringValue = ((StringValue) value).getValue();
        builder().setInvalidText(stringValue);
    }

    public static final class Repository
            extends ProjectionRepository<WatchId, ViolationsWatch, InvalidText> {

        @OverridingMethodsMustInvokeSuper
        @Override
        protected void setupEventRouting(EventRouting<WatchId> routing) {
            super.setupEventRouting(routing);
            routing.route(ConstraintViolated.class, (m, c) -> withId(DEFAULT));
        }
    }
}
