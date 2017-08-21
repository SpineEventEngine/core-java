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

package io.spine.server.entity;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.server.aggregate.AggregatePart;
import io.spine.test.entity.number.NaturalNumber;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static io.spine.server.model.EntityClass.getConstructor;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    public void define_final_updateState_method() throws NoSuchMethodException {
        final Method updateState =
                AbstractEntity.class.getDeclaredMethod("updateState", Message.class);
        final int modifiers = updateState.getModifiers();
        assertTrue(Modifier.isFinal(modifiers));
    }

    @Test
    public void prevent_validate_overriding() throws NoSuchMethodException {
        final Method validate =
                AbstractEntity.class.getDeclaredMethod("validate", Message.class);
        final int modifiers = validate.getModifiers();
        assertTrue(Modifier.isPrivate(modifiers) || Modifier.isFinal(modifiers));
    }

    @Test
    public void throw_InvalidEntityStateException_if_state_is_invalid() {
        final NaturalNumberEntity entity = new NaturalNumberEntity(0L);
        final NaturalNumber invalidNaturalNumber = newNaturalNumber(-1);
        try {
            // This should pass.
            entity.updateState(newNaturalNumber(1));

            // This should fail.
            entity.updateState(invalidNaturalNumber);

            fail("Exception expected.");
        } catch (InvalidEntityStateException e) {
            assertSize(1, e.getError()
                           .getValidationError()
                           .getConstraintViolationList());
        }
    }

    @SuppressWarnings("ConstantConditions") // The goal of the test.
    @Test(expected = NullPointerException.class)
    public void not_accept_null_to_checkEntityState() {
        final AnEntity entity = new AnEntity(0L);
        entity.checkEntityState(null);
    }

    @Test
    public void allow_valid_state() {
        final AnEntity entity = new AnEntity(0L);
        assertTrue(entity.checkEntityState(StringValue.getDefaultInstance())
                         .isEmpty());
    }

    @Test
    public void return_sting_ID() {
        final AnEntity entity = new AnEntity(1_234_567L);

        assertEquals("1234567", entity.stringId());
        assertSame(entity.stringId(), entity.stringId());
    }

    private static class AnEntity extends AbstractEntity<Long, StringValue> {
        protected AnEntity(Long id) {
            super(id);
        }
    }

    private static class NaturalNumberEntity extends AbstractEntity<Long, NaturalNumber> {
        private NaturalNumberEntity(Long id) {
            super(id);
        }
    }

    private static NaturalNumber newNaturalNumber(int value) {
        return NaturalNumber.newBuilder()
                            .setValue(value)
                            .build();
    }
}
