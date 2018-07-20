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

import com.google.common.base.Predicate;
import com.google.protobuf.Message;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An entity which has {@linkplain LifecycleFlags lifecycle flags}.
 *
 * @author Alexander Yevsyukov
 */
public interface EntityWithLifecycle<I, S extends Message> extends Entity<I, S> {

    /**
     * Obtains the current visibility status.
     */
    LifecycleFlags getLifecycleFlags();

    /**
     * Shows if current instance is archived or not.
     */
    @Column
    boolean isArchived();

    /**
     * Shows if current instance is deleted or not.
     */
    @Column
    boolean isDeleted();

    /**
     * Tells whether visibility of the entity changed since its initialization.
     */
    boolean lifecycleFlagsChanged();

    /**
     * Collection of predicates for filtering entities with lifecycle flags.
     */
    class Predicates {

        private static final Predicate<LifecycleFlags> isEntityVisible =
                new Predicate<LifecycleFlags>() {
                    @Override
                    public boolean apply(@Nullable LifecycleFlags input) {
                        return input == null ||
                                !(input.getArchived() || input.getDeleted());
                    }
                };

        private static final Predicate<EntityRecord> isRecordVisible =
                new Predicate<EntityRecord>() {

                    @Override
                    public boolean apply(@Nullable EntityRecord input) {
                        if (input == null) {
                            return true;
                        }
                        LifecycleFlags flags = input.getLifecycleFlags();
                        boolean result = isEntityVisible.apply(flags);
                        return result;
                    }
                };

        private static final Predicate<EntityRecordWithColumns> isRecordWithColumnsVisible =
                new Predicate<EntityRecordWithColumns>() {
                    @Override
                    public boolean apply(@Nullable EntityRecordWithColumns input) {
                        if (input == null) {
                            return false;
                        }
                        EntityRecord record = input.getRecord();
                        return isRecordVisible().apply(record);
                    }
                };

        private Predicates() {
            // Prevent instantiation of this utility class.
        }

        /**
         * Obtains the predicate for checking if an entity has
         * any of the {@link LifecycleFlags} set.
         *
         * <p>If so, an entity becomes "invisible" to load methods of a repository.
         * Entities with flags set must be treated by special {@linkplain RepositoryView views}
         * of a repository.
         *
         * @return the filter predicate
         * @see LifecycleFlags
         */
        public static Predicate<LifecycleFlags> isEntityVisible() {
            return isEntityVisible;
        }

        /**
         * Obtains the predicate for checking if an entity record has any
         * of the {@link LifecycleFlags} set.
         *
         * @return the predicate that filters "invisible" {@code EntityStorageRecord}s
         * @see EntityRecord#getLifecycleFlags()
         */
        public static Predicate<EntityRecord> isRecordVisible() {
            return isRecordVisible;
        }

        public static Predicate<EntityRecordWithColumns> isRecordWithColumnsVisible() {
            return isRecordWithColumnsVisible;
        }
    }
}
