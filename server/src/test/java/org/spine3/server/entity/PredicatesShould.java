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

package org.spine3.server.entity;

import org.junit.Test;
import org.spine3.test.Tests;

import static org.junit.Assert.assertFalse;
import static org.spine3.server.entity.Predicates.isEntityVisible;
import static org.spine3.server.entity.Predicates.isRecordVisible;

/**
 * @author Alexander Yevsyukov
 */
public class PredicatesShould {

    @Test
    public void have_private_default_ctor() {
        Tests.hasPrivateParameterlessCtor(Predicates.class);
    }

    @Test
    public void consider_archived_entity_invisible() {
        final Visibility status =
                Visibility.newBuilder()
                            .setArchived(true)
                            .build();
        assertFalse(isEntityVisible().apply(status));
    }

    @Test
    public void consider_deleted_entity_invisible() {
        final Visibility status =
                Visibility.newBuilder()
                            .setDeleted(true)
                            .build();
        assertFalse(isEntityVisible().apply(status));
    }

    @Test
    public void consider_archived_record_invisible() {
        final EntityRecord record =
                EntityRecord
                        .newBuilder()
                        .setVisibility(Visibility.newBuilder()
                                                     .setArchived(true))
                        .build();
        assertFalse(isRecordVisible().apply(record));
    }

    @Test
    public void consider_deleted_record_invisible() {
        final EntityRecord record =
                EntityRecord
                        .newBuilder()
                        .setVisibility(Visibility.newBuilder()
                                                     .setDeleted(true))
                        .build();
        assertFalse(isRecordVisible().apply(record));
    }
}
