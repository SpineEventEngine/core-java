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
import com.google.protobuf.Timestamp;
import org.spine3.AggregateId;
import org.spine3.util.ClassName;
import org.spine3.util.Messages;
import org.spine3.util.TypeName;
import org.spine3.util.TypeToClassMap;

import static org.spine3.gae.datastore.DataStoreStorage.*;

/**
 * Abstract implementation base for entity converters.
 *
 * @param <T> the type of messages the converter handles
 * @param <I> the type of the IDs for the messages
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
abstract class BaseConverter<T extends Message, I extends Message> implements Converter<T> {

    private final TypeName typeName;

    protected BaseConverter(TypeName typeName) {
        this.typeName = typeName;
    }

    protected void setTimestamp(Entity entity, Timestamp timestamp) {
        //TODO:2015-07-27:alexander.yevsyukov: Store as one field.
        entity.setProperty(TIMESTAMP_KEY, timestamp.getSeconds());
        entity.setProperty(TIMESTAMP_NANOS_KEY, timestamp.getNanos());
    }

    //TODO:2015-07-27:alexander.yevsyukov: Move constants closer to the usage.
    // It looks like they don't really belong to the DataStoreStorage class.

    protected void setValue(Entity entity, T value) {
        final Any any = Messages.toAny(value);
        final Blob blob = Converters.toBlob(any);
        entity.setProperty(VALUE_KEY, blob);
    }

    protected void setType(Entity entity) {
        entity.setProperty(TYPE_URL_KEY, getTypeName());
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
        //TODO:2015-07-24:alexander.yevsyukov: Why do we use Java class name here and not a Proto type?
        // What if we read this store from another language like Go?
        // Also notice that this lookup is going to be done every time we create an entity.
        final ClassName className = TypeToClassMap.get(typeName);
        return className.toString();
    }
}