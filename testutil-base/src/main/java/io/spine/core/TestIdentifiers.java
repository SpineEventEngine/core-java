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

package io.spine.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.Identifier.newUuid;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;

/**
 * Collection of factory methods for creating identifiers for tests.
 *
 * @author Alexander Yevsyukov
 */
public class TestIdentifiers {

    /**
     * The prefix for generated tenant identifiers.
     */
    private static final String TENANT_PREFIX = "tenant-";
    
    /**
     * The prefix for generated user identifiers.
     */
    private static final String USER_PREFIX = "user-";

    private TestIdentifiers() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates a new user ID instance by passed string value.
     *
     * @param value new user ID value
     * @return new instance
     */
    public static UserId newUserId(String value) {
        checkNotNull(value);

        return UserId.newBuilder()
                .setValue(value)
                .build();
    }

    /**
     * Generates a new UUID-based {@code UserId}.
     */
    public static UserId newUserUuid() {
        return newUserId(USER_PREFIX + newUuid());
    }

    /**
     * Generates a new UUID-based {@code TenantId}.
     */
    public static TenantId newTenantUuid() {
        return newTenantId(TENANT_PREFIX + newUuid());
    }

    /**
     * Creates a new {@code TenantId} with the passed value.
     *
     * @param value must be non-null, not empty, and not-blank
     * @return new {@code TenantId}
     */
    public static TenantId newTenantId(String value) {
        checkNotEmptyOrBlank(value, TenantId.class.getSimpleName());
        return TenantId.newBuilder()
                       .setValue(value)
                       .build();
    }

    /**
     * Creates a test instance of {@code TenantId} with the simple name of the passed test class.
     */
    public static TenantId newTenantId(Class<?> testClass) {
        return newTenantId(testClass.getSimpleName());
    }

}
