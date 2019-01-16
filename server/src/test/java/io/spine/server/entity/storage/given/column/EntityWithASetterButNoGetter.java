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

package io.spine.server.entity.storage.given.column;

import com.google.protobuf.Any;
import io.spine.server.entity.AbstractEntity;

/**
 * An entity type which declares a {@linkplain #setSecretNumber(Integer) mutator method},
 * however doesn't declare a respective accessor method.
 *
 * <p>{@code ColumnReader} should not get confused and assume that the mutator method is
 * a property, and, therefore, a potential column.
 */
public class EntityWithASetterButNoGetter extends AbstractEntity<String, Any> {

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private Integer secretNumber;

    protected EntityWithASetterButNoGetter(String id) {
        super(id);
    }

    @SuppressWarnings({"WeakerAccess", "unused"}) // Required for a test
    public void setSecretNumber(Integer secretNumber) {
        this.secretNumber = secretNumber;
    }
}
