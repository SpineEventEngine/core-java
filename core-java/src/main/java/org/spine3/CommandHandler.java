/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */

package org.spine3;

import com.google.common.eventbus.Subscribe;
import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;

import java.util.List;

/**
 * Interface for command handler classes.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 */
public interface CommandHandler<T extends Message> {

    /**
     * Handles incoming command of the {@link T} type.
     *
     * @param command the command to handle
     * @param context the context of the command
     * @return a list of the event records
     */
    @Subscribe
    List<EventRecord> handle(T command, CommandContext context);

}
