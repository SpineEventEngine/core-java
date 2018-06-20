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

package io.spine.server.entity;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.test.entity.number.NaturalNumber;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Illia Shepilov
 */
public class AbstractEntityShould {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Ensures that {@link AbstractEntity#updateState(Message)} is final so that
     * it's not possible to override the default behaviour.
     */
    @Test
    @DisplayName("prevent updateState method overriding")
    void preventUpdateStateMethodOverriding() throws NoSuchMethodException {
        final Method updateState =
                AbstractEntity.class.getDeclaredMethod("updateState", Message.class);
        final int modifiers = updateState.getModifiers();
        assertTrue(Modifier.isFinal(modifiers));
    }

    /**
     * Ensures that {@link AbstractEntity#validate(Message)} is final so that
     * it's not possible to override the default behaviour.
     */
    @Test
    @DisplayName("prevent validate method overriding")
    void preventValidateMethodOverriding() throws NoSuchMethodException {
        final Method validate =
                AbstractEntity.class.getDeclaredMethod("validate", Message.class);
        final int modifiers = validate.getModifiers();
        assertTrue(Modifier.isPrivate(modifiers) || Modifier.isFinal(modifiers));
    }

    @Test
    @DisplayName("throw InvalidEntityStateException if state is invalid")
    void throwInvalidEntityStateExceptionIfStateIsInvalid() {
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
    @Test
    @DisplayName("not accept null to checkEntityState")
    void notAcceptNullToCheckEntityState() {
        AnEntity entity = new AnEntity(0L);

        thrown.expect(NullPointerException.class);
        entity.checkEntityState(null);
    }

    @Test
    @DisplayName("allow valid state")
    void allowValidState() {
        final AnEntity entity = new AnEntity(0L);
        assertTrue(entity.checkEntityState(StringValue.getDefaultInstance())
                         .isEmpty());
    }

    @Test
    @DisplayName("return sting ID")
    void returnStingID() {
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
