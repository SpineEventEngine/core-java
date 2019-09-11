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

package io.spine.server.delivery;

import com.google.common.testing.NullPointerTester;
import io.spine.server.model.ModelError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("DeliveryErrors should")
class DeliveryErrorsTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void nulls() {
        DeliveryErrors.Builder builder = DeliveryErrors.newBuilder();
        new NullPointerTester()
                .testInstanceMethods(builder, PACKAGE);
    }

    @Test
    @DisplayName("accept model errors")
    void acceptErrors() {
        DeliveryErrors.Builder builder = DeliveryErrors.newBuilder();
        ModelError mockError = new ModelError("boo!");
        builder.addError(mockError);
        DeliveryErrors errors = builder.build();

        ModelError error = assertThrows(ModelError.class, errors::throwIfAny);
        assertThat(error)
                .isEqualTo(mockError);
    }

    @Test
    @DisplayName("accept runtime exceptions")
    void acceptExceptions() {
        DeliveryErrors.Builder builder = DeliveryErrors.newBuilder();
        RuntimeException mockException = new IllegalArgumentException("42");
        builder.addException(mockException);
        DeliveryErrors errors = builder.build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          errors::throwIfAny);
        assertThat(exception)
                .isEqualTo(mockException);
    }

    @Test
    @DisplayName("throw exceptions with suppressed throwables")
    void throwWithSuppressed() {
        DeliveryErrors.Builder builder = DeliveryErrors.newBuilder();
        ModelError mockError = new ModelError("model is wrong");
        ModelError anotherMockError = new ModelError("completely");
        RuntimeException mockException = new IllegalArgumentException("3.14");
        builder.addError(mockError);
        builder.addError(anotherMockError);
        builder.addException(mockException);

        DeliveryErrors errors = builder.build();
        ModelError error = assertThrows(ModelError.class, errors::throwIfAny);
        assertThat(error)
                .isEqualTo(mockError);
        assertThat(error.getSuppressed())
                .asList()
                .containsExactly(anotherMockError, mockException);
    }
}
