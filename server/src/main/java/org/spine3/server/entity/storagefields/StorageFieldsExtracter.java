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

package org.spine3.server.entity.storagefields;

import org.spine3.server.entity.Entity;
import org.spine3.server.entity.StorageFields;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmytro Dashenkov.
 */
public class StorageFieldsExtracter {

    private static final Map<Class<? extends Entity<?, ?>>, StorageFieldsGenerator> fieldGenerators
            = new ConcurrentHashMap<>();

    public static StorageFields extract(Entity<?, ?> entity) {
        checkNotNull(entity);
        final Class<? extends Entity> entityClass = entity.getClass();
        @SuppressWarnings("SuspiciousMethodCalls") // Generic parameters mismatch
        StorageFieldsGenerator fieldsGenerator = fieldGenerators.get(entityClass);
        if (fieldsGenerator == null) {
            fieldsGenerator = newGenerator(entityClass);
        }
        final StorageFields fields = fieldsGenerator.apply(entity);
        return fields;
    }

    private static StorageFieldsGenerator newGenerator(Class<? extends Entity> entityClass) {
        return new StorageFieldsGenerator();
    }
}
