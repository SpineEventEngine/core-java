/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.gae.datastore;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.TimestampOrBuilder;
import org.spine3.TypeName;
import org.spine3.protobuf.Messages;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.aggregate.AggregateId;

import static org.spine3.gae.datastore.DataStoreHelper.*;

/**
 * Abstract implementation base for entity converters.
 *
 * @param <T> the type of messages the converter handles
 * @param <I> the type of the IDs for the messages
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
abstract class BaseConverter<I extends Message, T extends Message> implements Converter<T> {

    private final TypeName typeName;

    protected BaseConverter(TypeName typeName) {
        this.typeName = typeName;
    }

    protected void setTimestamp(Entity entity, TimestampOrBuilder timestamp) {
        entity.setProperty(TIMESTAMP_KEY, Timestamps.convertToDate(timestamp));
    }

    protected void setValue(Entity entity, T value) {
        final Any any = Messages.toAny(value);
        final Blob blob = Converters.toBlob(any);
        entity.setProperty(VALUE_KEY, blob);
    }

    protected void setType(Entity entity) {
        entity.setProperty(TYPE_KEY, getTypeName());
    }

    protected void setAggregateId(Entity entity, AggregateId aggregateId) {
        entity.setProperty(AGGREGATE_ID_KEY, aggregateId.toString());
    }

    protected void setVersion(Entity entity, int version) {
        entity.setProperty(VERSION_KEY, version);
    }

    protected abstract Entity newEntity(I id);

    protected String getTypeName() {
        return typeName.toString();
    }

    protected String getEntityKind() {
        return typeName.toString();
    }
}