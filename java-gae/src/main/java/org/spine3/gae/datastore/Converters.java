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
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.AnyOrBuilder;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.AggregateCommand;
import org.spine3.AggregateId;
import org.spine3.base.*;
import org.spine3.util.Commands;
import org.spine3.util.Events;
import org.spine3.util.Messages;
import org.spine3.util.TypeName;

import java.util.Map;

import static org.spine3.gae.datastore.DataStoreStorage.*;

/**
 * Holds Entity Converters and provides an API for them.
 *
 * @author Mikhayil Mikhaylov
 */
@SuppressWarnings("UtilityClass")
class Converters {

    private static final Map<Class<?>, Converter<?>> map = ImmutableMap.<Class<?>, Converter<?>>builder()
            .put(CommandRequest.class, new CommandRequestConverter())
            .put(EventRecord.class, new EventRecordConverter())
            .build();

    private Converters() {
        // Prevent instantiation of this utility class.
    }

    public static Entity convert(Message message) {
        final Class<?> messageClass = message.getClass();

        final Converter converter = map.get(messageClass);
        if (converter == null) {
            throw new IllegalArgumentException("Unable to find entity converter for the message class: " + messageClass.getName());
        }

        @SuppressWarnings("unchecked") // We ensure type safety by having the private map of converters in the map initialization code above.
        final Entity result = converter.convert(message);
        return result;
    }

    /**
     * Creates new {@code Blob} with the {@code ByteArray} content from the passed 'any' instance.
     * @param any the instance to convert
     * @return new {@code Blob}
     */
    public static Blob toBlob(AnyOrBuilder any) {
        return new Blob(any.getValue().toByteArray());
    }

    /**
     * Converts Protobuf messages to DataStore CommandRequest entities.
     */
    static class CommandRequestConverter extends BaseConverter<CommandRequest> {

        CommandRequestConverter() {
            super(TypeName.of(CommandRequest.getDescriptor()));
        }

        @Override
        public Entity convert(CommandRequest commandRequest) {
            final AggregateCommand command = AggregateCommand.of(commandRequest);
            final AggregateId aggregateId = command.getAggregateId();
            final CommandContext commandContext = commandRequest.getContext();
            final CommandId commandId = commandContext.getCommandId();
            final String id = Commands.idToString(commandId);
            final Timestamp timestamp = commandId.getTimestamp();

            final Entity entity = new Entity(getEntityKind(), id);

            final Any any = Messages.toAny(commandRequest);

            entity.setProperty(VALUE_KEY, toBlob(any));
            entity.setProperty(TYPE_URL_KEY, getTypeName());
            entity.setProperty(AGGREGATE_ID_KEY, aggregateId.toString());
            //TODO:2015-07-27:alexander.yevsyukov: Store as one field.
            entity.setProperty(TIMESTAMP_KEY, timestamp.getSeconds());
            entity.setProperty(TIMESTAMP_NANOS_KEY, timestamp.getNanos());

            return entity;
        }

    }

    /**
     * Converts EventRecord messages to DataStore entities.
     *
     * @author Mikhail Mikhaylov
     */
    static class EventRecordConverter extends BaseConverter<EventRecord> {

        EventRecordConverter() {
            super(TypeName.of(EventRecord.getDescriptor()));
        }

        @Override
        public Entity convert(EventRecord eventRecord) {
            final EventContext eventContext = eventRecord.getContext();
            final EventId eventId = eventContext.getEventId();
            final String id = Events.idToString(eventId);
            final AggregateId aggregateId = AggregateId.of(eventContext);
            final Timestamp timestamp = eventId.getTimestamp();
            final int version = eventContext.getVersion();

            final Entity entity = new Entity(getEntityKind(), id);

            final Any any = Messages.toAny(eventRecord);

            entity.setProperty(VALUE_KEY, toBlob(any));
            entity.setProperty(TYPE_URL_KEY, getTypeName());
            entity.setProperty(AGGREGATE_ID_KEY, aggregateId.toString());
            //TODO:2015-07-27:alexander.yevsyukov: Store as one field.
            entity.setProperty(TIMESTAMP_KEY, timestamp.getSeconds());
            entity.setProperty(TIMESTAMP_NANOS_KEY, timestamp.getNanos());
            entity.setProperty(VERSION_KEY, version);

            return entity;
        }

    }

}