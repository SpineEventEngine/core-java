/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.EventRecord;
import org.spine3.engine.Media;

import java.util.List;

/**
 * Stores and loads the events.
 *
 * @author Mikhail Melnik
 */
public class EventStore {

    private final Media media;

    public EventStore(Media media) {
        this.media = media;
    }

    /**
     * Loads all events for the given aggregate root id.
     *
     * @param aggregateRootId the id of the aggregate root
     * @return list of commands for the aggregate root
     */
    public List<? extends Message> load(Message aggregateRootId) {
        List<? extends Message> result = media.readEvents(aggregateRootId);
        return result;
    }

    /**
     * Stores the event record.
     *
     * @param record event record to store
     */
    public void store(EventRecord record) {
        media.writeEvent(record);
    }

    public List<EventRecord> getEvents() {
        List<EventRecord> result = media.readAllEvents();
        return result;
    }

    ;

    public List<EventRecord> getEvents(Timestamp timestamp) {
        List<EventRecord> result = media.readEvents(timestamp);
        return result;
    }

    ;

}
