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

package io.spine.server.tenant;

import io.spine.core.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.core.given.GivenTenantId.nameOf;
import static io.spine.test.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Tests.nullRef;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 * @author Alexander Litus
 */
@DisplayName("CurrentTenant should")
class CurrentTenantTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(CurrentTenant.class);
    }

    @Test
    @DisplayName("reject null value")
    void rejectNullValue() {
        assertThrows(NullPointerException.class, () -> CurrentTenant.set(nullRef()));
    }

    @Test
    @DisplayName("reject default value")
    void rejectDefaultValue() {
        assertThrows(IllegalArgumentException.class,
                     () -> CurrentTenant.set(TenantId.getDefaultInstance()));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We check isPresent() in assertion.
    @Test
    @DisplayName("keep set value")
    void keepSetValue() {
        TenantId expected = nameOf(getClass());

        CurrentTenant.set(expected);

        Optional<TenantId> currentTenant = CurrentTenant.get();
        assertTrue(currentTenant.isPresent());
        assertEquals(expected, currentTenant.get());
    }

    @Test
    @DisplayName("clear set value")
    void clearSetValue() {
        TenantId value = nameOf(getClass());
        CurrentTenant.set(value);

        CurrentTenant.clear();

        assertFalse(CurrentTenant.get().isPresent());
    }
}
