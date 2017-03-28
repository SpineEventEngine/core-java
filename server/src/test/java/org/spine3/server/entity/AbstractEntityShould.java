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

package org.spine3.server.entity;

import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.server.aggregate.AggregatePart;
import org.spine3.test.Tests;

import static org.junit.Assert.assertTrue;
import static org.spine3.server.entity.AbstractEntity.getConstructor;
import static org.spine3.test.Tests.newUuidValue;

/**
 * @author Illia Shepilov
 */
public class AbstractEntityShould {

    @SuppressWarnings("unchecked")
    // Supply a "wrong" value on purpose to cause the validation failure.
    @Test(expected = IllegalStateException.class)
    public void throw_exception_when_aggregate_does_not_have_appropriate_constructor() {
        getConstructor(AggregatePart.class, String.class);
    }

    @Test
    public void accept_anything_to_isValidate() {
        final AnEntity entity = new AnEntity(0L);

        assertTrue(entity.isValid(Tests.<StringValue>nullRef()));
        assertTrue(entity.isValid(StringValue.getDefaultInstance()));
        assertTrue(entity.isValid(newUuidValue()));
    }

    @Test(expected = IllegalStateException.class)
    public void not_allow_invalid_state() {
        final NonblankString entity = new NonblankString(1L);

        // This should pass.
        entity.updateState(newUuidValue());

        // This should fail.
        entity.updateState(StringValue.getDefaultInstance());
    }

    private static class AnEntity extends AbstractEntity<Long, StringValue> {
        protected AnEntity(Long id) {
            super(id);
        }
    }

    private static class NonblankString extends AbstractEntity<Long, StringValue> {
        protected NonblankString(Long id) {
            super(id);
        }

        @Override
        protected boolean isValid(StringValue newState) {
            return !newState.getValue().isEmpty();
        }
    }
}
