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

package org.spine3.server.storage;

import org.junit.Test;
import org.spine3.test.Tests;

import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Yevsyukov
 */
public class PredicatesShould {

    @Test
    public void have_private_default_ctor() {
        Tests.hasPrivateParameterlessCtor(Predicates.class);
    }

    @Test
    public void consider_archived_invisible() {
        assertFalse(Predicates.isVisible()
                              .apply(EntityStorageRecord.newBuilder()
                                                        .setArchived(true)
                                                        .build()));
    }

    @Test
    public void consider_deleted_invisible() {
        assertFalse(Predicates.isVisible()
                              .apply(EntityStorageRecord.newBuilder()
                                                        .setDeleted(true)
                                                        .build()));
    }
}
