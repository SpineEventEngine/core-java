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
package org.spine3.sample.store.filesystem;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.server.StorageWithTimelineAndVersion;
import org.spine3.util.*;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Test file system based implementation of the {@link Message} repository.
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public class FileSystemStorage<M extends Message> implements StorageWithTimelineAndVersion<M> {

    private static final Map<Class<?>, FilteringHelper<?>> helpers = ImmutableMap.<Class<?>, FilteringHelper<?>>builder()
            .put(CommandRequest.class, new CommandFilteringHelper())
            .put(EventRecord.class, new EventFilteringHelper())
            .build();

    private final Class<M> clazz;

    public static <M extends Message> FileSystemStorage<M> newInstance(Class<M> messageClass) {
        return new FileSystemStorage<>(messageClass);
    }

    private FileSystemStorage(Class<M> clazz) {
        this.clazz = clazz;
    }


    private static <T> ImmutableList<T> filter(Iterable<T> list, Predicate<T> predicate) {
        return FluentIterable.from(list).filter(predicate).toList();
    }

    @Override
    public List<M> read(Message parentId, int sinceVersion) {
        checkNotNull(parentId);

        final List<M> messages = FileSystemHelper.read(clazz, parentId);

        //noinspection unchecked
        final FilteringHelper<M> helper = (FilteringHelper<M>) helpers.get(clazz);
        final Predicate<M> predicate = helper.getSinceVersionPredicate(sinceVersion);
        final ImmutableList<M> result = filter(messages, predicate);
        return result;
    }

    @Override
    public List<M> read(Timestamp from) {
        checkNotNull(from);

        final List<M> messages = FileSystemHelper.readAll(clazz);

        //noinspection unchecked
        final FilteringHelper<M> helper = (FilteringHelper<M>) helpers.get(clazz);
        final Predicate<M> predicate = helper.getWereAfterPredicate(from);
        final ImmutableList<M> result = filter(messages, predicate);
        return result;
    }

    @Override
    public List<M> read(Message parentId, Timestamp from) {
        checkNotNull(from);
        checkNotNull(parentId);

        final List<M> messages = FileSystemHelper.read(clazz, parentId);

        //noinspection unchecked
        final FilteringHelper<M> helper = (FilteringHelper<M>) helpers.get(clazz);
        final Predicate<M> predicate = helper.getWereAfterPredicate(from);
        final ImmutableList<M> result = filter(messages, predicate);
        return result;
    }

    @Override
    public List<M> read(Message parentId) {
        checkNotNull(parentId);

        final List<M> messages = FileSystemHelper.read(clazz, parentId);

        return messages;
    }

    @Override
    public List<M> readAll() {
        final List<M> messages = FileSystemHelper.readAll(clazz);
        //noinspection unchecked
        final FilteringHelper<M> helper = (FilteringHelper<M>) helpers.get(clazz);
        helper.sort(messages);
        return messages;
    }

    @Override
    public void store(Message message) {
        FileSystemHelper.write(message);
    }

    private static class CommandFilteringHelper implements FilteringHelper<CommandRequest> {

        private static final String COMMANDS_DO_NOT_SUPPORT_VERSIONS = "Commands don\'t support versions";

        @Override
        public Predicate<CommandRequest> getWereAfterPredicate(Timestamp from) {
            return Commands.wereAfter(from);
        }

        @Override
        public Predicate<CommandRequest> getSinceVersionPredicate(int sinceVersion) {
            throw new IllegalStateException(COMMANDS_DO_NOT_SUPPORT_VERSIONS);
        }

        @Override
        public void sort(List<CommandRequest> messages) {
            Commands.sort(messages);
        }
    }

    private static class EventFilteringHelper implements FilteringHelper<EventRecord> {

        @Override
        public Predicate<EventRecord> getWereAfterPredicate(Timestamp from) {
            return Events.getEventPredicate(from);
        }

        @Override
        public Predicate<EventRecord> getSinceVersionPredicate(int sinceVersion) {
            return Events.getEventPredicate(sinceVersion);
        }

        @Override
        public void sort(List<EventRecord> messages) {
            Events.sort(messages);
        }
    }

    private interface FilteringHelper<M extends Message> {

        Predicate<M> getWereAfterPredicate(Timestamp from);

        Predicate<M> getSinceVersionPredicate(int sinceVersion);

        void sort(List<M> messages);
    }
}
