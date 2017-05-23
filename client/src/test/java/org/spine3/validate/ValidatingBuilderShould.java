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
package org.spine3.validate;

import com.google.protobuf.Empty;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Alex Tymchenko
 */
public class ValidatingBuilderShould {

    @Test
    public void have_TypeInfo_utility_class() {
        assertHasPrivateParameterlessCtor(ValidatingBuilder.TypeInfo.class);
    }

    @Test
    public void allow_accessing_newBuilder_method_via_TypeInfo() {
        assertNotNull(ValidatingBuilder.TypeInfo.getNewBuilderMethod(TestValidatingBuilder.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_if_accessing_newBuilder_method_for_improperly_declared_builder() {
        ValidatingBuilder.TypeInfo.getNewBuilderMethod(WrongValidatingBuilder.class);
    }

    public static class TestValidatingBuilder
            extends AbstractValidatingBuilder<Empty, Empty.Builder> {
        private TestValidatingBuilder() {
            super();
        }

        public static TestValidatingBuilder newBuilder() {
            return new TestValidatingBuilder();
        }
    }

    /**
     * By convention, all {@code ValidatingBuilder} descendants must declare static
     * {@code newBuilder()} factory method, returning an instance of the class.
     *
     * <p>This class is a descendant, which violates this convention.
     */
    public static class WrongValidatingBuilder
            extends AbstractValidatingBuilder<Empty, Empty.Builder> {
        private WrongValidatingBuilder() {
            super();
        }
    }
}
