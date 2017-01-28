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

package org.spine3.server.aggregate.storage;

import com.google.common.base.Predicate;

import javax.annotation.Nullable;

/**
 * Collection of predicates for filtering {@link AggregateStorageRecord}s.
 *
 * @author Alexander Yevsyukov
 */
public class Predicates {

    public static final Predicate<AggregateStatus> isArchived = new Predicate<AggregateStatus>() {
        @Override
        public boolean apply(@Nullable AggregateStatus input) {
            return input != null && input.getArchived();
        }
    };

    public static final Predicate<AggregateStatus> isDeleted = new Predicate<AggregateStatus>() {
        @Override
        public boolean apply(@Nullable AggregateStatus input) {
            return input != null && input.getDeleted();
        }
    };

    public static final Predicate<AggregateStatus> isVisible = new Predicate<AggregateStatus>() {
        @Override
        public boolean apply(@Nullable AggregateStatus input) {
            return input == null ||
                    !(input.getArchived() || input.getDeleted());
        }
    };

    private Predicates() {
        // Prevent instantiation of this utility class.
    }
}
