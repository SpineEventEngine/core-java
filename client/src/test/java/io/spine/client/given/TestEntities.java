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

package io.spine.client.given;

import io.spine.test.client.TestEntityId;

import static java.util.concurrent.ThreadLocalRandom.current;

/**
 * A set of utilities working with test entities.
 */
public final class TestEntities {

    /**
     * Prevents the utility class instantiation.
     */
    private TestEntities() {
    }

    /**
     * Creates a new random {@code TestEntityId}.
     *
     * <p>The numeric value of the ID is between {@code -1000} (inclusive)
     * and {@code 1000} (exclusive) and is never equal to {@code 0};
     *
     * @return new random ID
     */
    public static TestEntityId randomId() {
        int randomNumber = current().nextInt(-1000, 1000);
        randomNumber = randomNumber != 0
                       ? randomNumber
                       : 314;
        return TestEntityId.newBuilder()
                           .setValue(randomNumber)
                           .build();
    }
}
