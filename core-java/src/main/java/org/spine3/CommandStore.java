/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

package org.spine3;

import com.google.protobuf.Message;
import org.spine3.base.CommandRequest;
import org.spine3.engine.Media;

import java.util.List;

/**
 * Stores and loads the commands.
 *
 * @author Mikhail Melnik
 */
public class CommandStore {

    private Media media;

    public CommandStore(Media media) {
        this.media = media;
    }

    /**
     * Loads all commands for the given aggregate root id.
     *
     * @param aggregateRootId the id of the aggregate root
     * @return list of commands for the aggregate root
     */
    List<CommandRequest> load(Message aggregateRootId) {
        return media.readCommands(aggregateRootId);
    }

    /**
     * Stores the command request.
     *
     * @param request command request to store
     */
    void store(CommandRequest request) {
        media.writeCommand(request);
    }

}
