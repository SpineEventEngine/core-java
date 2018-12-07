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

import io.spine.server.entity.storage.EntityRecordWithColumns;

import java.util.function.Predicate;

/**
 * Collection of predicates for filtering entities with lifecycle flags.
 */
public final class LifecyclePredicates {

    private static final Predicate<LifecycleFlags> isEntityActive =
            input -> input == null ||
                    !(input.getArchived() || input.getDeleted());

    private static final Predicate<EntityRecord> isRecordActive =
            input -> {
                if (input == null) {
                    return true;
                }
                LifecycleFlags flags = input.getLifecycleFlags();
                boolean result = isEntityActive.test(flags);
                return result;
            };

    private static final Predicate<EntityRecordWithColumns> isRecordWithColumnsActive =
            input -> {
                if (input == null) {
                    return false;
                }
                EntityRecord record = input.getRecord();
                return isRecordActive().test(record);
            };

    /** Prevent instantiation of this utility class. */
    private LifecyclePredicates() {
    }

    /**
     * Obtains the predicate for checking if an entity has
     * any of the {@link LifecycleFlags} set.
     *
     * <p>If so, an entity becomes inactive to load methods of a repository.
     * Entities with flags set must be treated by special
     * {@linkplain RepositoryView views} of a repository.
     *
     * @return the filter predicate
     * @see LifecycleFlags
     */
    public static Predicate<LifecycleFlags> isEntityActive() {
        return isEntityActive;
    }

    /**
     * Obtains the predicate for checking if an entity record has any
     * of the {@link LifecycleFlags} set.
     *
     * @return the predicate that filters inactive {@link EntityRecord}s
     * @see EntityRecord#getLifecycleFlags()
     */
    public static Predicate<EntityRecord> isRecordActive() {
        return isRecordActive;
    }

    /**
     * Obtains the predicate for checking if an
     * {@link io.spine.server.entity.storage.EntityRecordWithColumns entity record
     * with columns} has any of the {@link LifecycleFlags} set.
     *
     * @return the predicate that filters inactive
     * {@link io.spine.server.entity.storage.EntityRecordWithColumns}s
     * @see EntityRecord#getLifecycleFlags()
     */
    public static Predicate<EntityRecordWithColumns> isRecordWithColumnsActive() {
        return isRecordWithColumnsActive;
    }
}
