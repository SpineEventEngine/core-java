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

import com.google.common.reflect.Invokable;
import com.google.common.testing.EqualsTester;
import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("AbstractVersionableEntity should")
class AbstractVersionableEntityTest {

    @SuppressWarnings("MagicNumber")
    @Test
    @DisplayName("support equality")
    void supportEquality() throws Exception {
        final AvEntity entity = new AvEntity(88L);
        final AvEntity another = new AvEntity(88L);
        another.updateState(entity.getState(), entity.getVersion());

        new EqualsTester().addEqualityGroup(entity, another)
                          .addEqualityGroup(new AvEntity(42L))
                          .testEquals();
    }

    @Test
    @Disabled // The `updateState` method was made public to be accessible from `testutil-server`
    @DisplayName("have `updateState` method visible to package only")
    void haveUpdateStatePackagePrivate() throws NoSuchMethodException {
        boolean methodFound = false;

        final Method[] methods = AbstractVersionableEntity.class.getDeclaredMethods();
        for (Method method : methods) {
            if ("updateState".equals(method.getName())) {
                final Invokable<?, Object> updateState = Invokable.from(method);
                assertTrue(updateState.isPackagePrivate());
                methodFound = true;
            }
        }
        assertTrue(methodFound,
                   "Cannot check 'updateState(...)' in " + AbstractVersionableEntity.class);
    }

    private static class AvEntity extends AbstractVersionableEntity<Long, StringValue> {
        protected AvEntity(Long id) {
            super(id);
        }
    }
}
