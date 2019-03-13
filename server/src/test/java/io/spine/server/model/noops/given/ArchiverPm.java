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

package io.spine.server.model.noops.given;

import io.spine.server.command.Assign;
import io.spine.server.model.Nothing;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.test.model.contexts.archiver.ArchiveFile;
import io.spine.test.model.contexts.archiver.Archiver;
import io.spine.test.model.contexts.archiver.ArchiverId;
import io.spine.test.model.contexts.archiver.ArchiverVBuilder;

/**
 * A test process manager which emits empty events and commands.
 */
public final class ArchiverPm extends ProcessManager<ArchiverId, Archiver, ArchiverVBuilder> {

    static final ArchiverId SINGLE_ID = ArchiverId.generate();

    private ArchiverPm(ArchiverId id) {
        super(id);
    }

    @Assign
    Nothing handle(ArchiveFile command) {
        ArchiverVBuilder builder = builder();
        builder.setId(SINGLE_ID)
               .setArchivedFiles(builder.getArchivedFiles() + 1);
        return Nothing.getDefaultInstance();
    }

    public static final class Repository
            extends ProcessManagerRepository<ArchiverId, ArchiverPm, Archiver> {
    }
}
