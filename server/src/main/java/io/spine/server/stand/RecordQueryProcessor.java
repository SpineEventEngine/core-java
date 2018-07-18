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

package io.spine.server.stand;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.Target;
import io.spine.core.Version;
import io.spine.server.entity.EntityRecord;

import java.util.Iterator;

/**
 * A common base for {@linkplain QueryProcessor query processors} targeting objects that are stored
 * in record-based repositories and storages.
 *
 * @author Alex Tymchenko
 * @author Dmytro Kuzmin
 */
abstract class RecordQueryProcessor implements QueryProcessor {

    @Override
    public ImmutableCollection<EntityStateWithVersion> process(Query query) {
        final Target target = query.getTarget();
        final FieldMask fieldMask = query.getFieldMask();
        final Iterator<EntityRecord> records = queryForRecords(target, fieldMask);
        final ImmutableList<EntityStateWithVersion> result = toQueryResult(records);
        return result;
    }

    private static ImmutableList<EntityStateWithVersion>
    toQueryResult(Iterator<EntityRecord> records) {
        final ImmutableList.Builder<EntityStateWithVersion> resultBuilder = ImmutableList.builder();
        while (records.hasNext()) {
            final EntityRecord entityRecord = records.next();
            final Any state = entityRecord.getState();
            final Version version = entityRecord.getVersion();
            final EntityStateWithVersion message = EntityStateWithVersion.newBuilder()
                                                                         .setState(state)
                                                                         .setVersion(version)
                                                                         .build();
            resultBuilder.add(message);
        }
        final ImmutableList<EntityStateWithVersion> result = resultBuilder.build();
        return result;
    }

    protected abstract Iterator<EntityRecord> queryForRecords(Target target, FieldMask fieldMask);
}
