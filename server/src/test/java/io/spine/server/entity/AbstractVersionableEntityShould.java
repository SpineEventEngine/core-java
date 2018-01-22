/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class AbstractVersionableEntityShould {

    @SuppressWarnings("MagicNumber")
    @Test
    public void have_equals() throws Exception {
        final AvEntity entity = new AvEntity(88L);
        final AvEntity another = new AvEntity(88L);
        another.updateState(entity.getState(), entity.getVersion());

        new EqualsTester().addEqualityGroup(entity, another)
                          .addEqualityGroup(new AvEntity(42L))
                          .testEquals();
    }

    @Test
    public void have_updateState_method_visible_to_package_only() throws NoSuchMethodException {
        boolean methodFound = false;

        final Method[] methods = AbstractVersionableEntity.class.getDeclaredMethods();
        for (Method method : methods) {
            if ("updateState".equals(method.getName())) {
                final Invokable<?, Object> updateState = Invokable.from(method);
                assertTrue(updateState.isPackagePrivate());
                methodFound = true;
            }
        }
        assertTrue("Cannot check 'updateState(...)' in " + AbstractVersionableEntity.class,
                   methodFound);
    }

    private static class AvEntity extends AbstractVersionableEntity<Long, StringValue> {
        protected AvEntity(Long id) {
            super(id);
        }
    }
}
