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

package org.spine3.server.command;

import com.google.common.base.Optional;
import org.spine3.base.CommandId;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.storage.CommandStatusRecord;
import org.spine3.server.entity.Repository;
import org.spine3.server.storage.Storage;
import org.spine3.server.storage.StorageFactory;

//TODO:2017-02-15:alexander.yevsyukov: Update Javadoc after migration to this class.
/**
 * This is Repository-based implementation of Command Store, which is going to
 * {@link CommandStore} .
 *
 * @author Alexander Yevsyukov
 */
public class CommandRepository extends Repository<CommandId, CommandEntity, CommandStatusRecord> {

    /**
     * {@inheritDoc}
     */
    protected CommandRepository(BoundedContext boundedContext) {
        super(boundedContext);
    }

    @Override
    public Optional<CommandEntity> load(CommandId id) {
        return null;
    }

    @Override
    protected void store(CommandEntity obj) {

    }

    @Override
    protected void updateMetadata(CommandId id, CommandStatusRecord metadata) {

    }

    @Override
    protected Storage createStorage(StorageFactory factory) {
        return null;
    }
}
