/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.storage.given;

import io.spine.server.ContextSpec;
import io.spine.server.storage.MessageRecordSpec;
import io.spine.server.storage.MessageStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.test.storage.StgProject;
import io.spine.test.storage.StgProjectId;

/**
 * A storage of the {@link io.spine.test.storage.StgProject} messages.
 *
 * <p>While the message itself is marked as an entity state, this storage operates with it
 * like it is an ordinary message record.
 *
 * <p>This storage defines several {@linkplain StgColumn custom columns} to use in tests.
 *
 * @see RecordStorageDelegateTest
 * @see StgColumn
 */
public class StgProjectStorage extends MessageStorage<StgProjectId, StgProject> {

    public StgProjectStorage(ContextSpec context, StorageFactory factory) {
        super(context, factory.createRecordStorage(context, spec()));
    }

    private static MessageRecordSpec<StgProjectId, StgProject> spec() {
        @SuppressWarnings("ConstantConditions")     // Proto getters return non-{@code null} values.
        MessageRecordSpec<StgProjectId, StgProject> spec =
                new MessageRecordSpec<>(StgProjectId.class,
                                        StgProject.class,
                                        StgProject::getId,
                                        StgColumn.definitions());
        return spec;
    }
}
