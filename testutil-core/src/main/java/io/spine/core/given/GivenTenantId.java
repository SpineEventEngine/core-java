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

package io.spine.core.given;

import io.spine.Identifier;
import io.spine.core.TenantId;

import static io.spine.validate.Validate.checkNotEmptyOrBlank;

/**
 * Collection of factory methods for creating identifiers for tests.
 *
 * @author Alexander Yevsyukov
 */
public class GivenTenantId {

    /**
     * The prefix for generated tenant identifiers.
     */
    private static final String TENANT_PREFIX = "tenant-";

    private GivenTenantId() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Generates a new UUID-based {@code TenantId}.
     */
    public static TenantId newUuid() {
        return of(TENANT_PREFIX + Identifier.newUuid());
    }

    /**
     * Creates a new {@code TenantId} with the passed value.
     *
     * @param value must be non-null, not empty, and not-blank
     * @return new {@code TenantId}
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")
    public static TenantId of(String value) {
        checkNotEmptyOrBlank(value, "value");
        return TenantId.newBuilder()
                       .setValue(value)
                       .build();
    }

    /**
     * Creates a test instance of {@code TenantId} with the simple name of the passed test class.
     */
    public static TenantId nameOf(Class<?> testClass) {
        return of(testClass.getSimpleName());
    }
}
