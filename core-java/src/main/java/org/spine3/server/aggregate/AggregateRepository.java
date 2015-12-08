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

package org.spine3.server.aggregate;

import com.google.protobuf.Message;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.server.MultiHandler;
import org.spine3.server.Repository;
import org.spine3.server.internal.CommandHandlingObject;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * The common interface for aggregate repositories.
 *
 * @param <I> the type of IDs of aggregates
 * @param <A> aggregate type
 * @author Alexander Yevsyukov
 */
public interface AggregateRepository<I, A extends Aggregate<I, ?>> extends Repository<I, A>, MultiHandler, CommandHandlingObject {

    /**
     * Loads or creates an aggregate with the passed ID.
     *
     * @param id the id of the aggregate
     * @return loaded or newly created instance of the aggregate
     */
    @SuppressWarnings("AbstractMethodOverridesAbstractMethod") /* We override the default behavior of loading. */
    @Nonnull
    @Override
    A load(I id);

    /**
     * Processes the command by dispatching it to a method of an aggregate.
     *
     * <p>For more details on writing aggregate commands read
     * <a href="http://github.com/SpineEventEngine/core/wiki/Writing-Aggregate-Commands">"Writing Aggregate Commands"</a>.
     *
     * @param command the command to dispatch
     * @param context context info of the command
     * @return a list of the event records
     * @throws InvocationTargetException if an exception occurs during command dispatching
     * @see <a href="http://github.com/SpineEventEngine/core/wiki/Writing-Aggregate-Commands">Writing Aggregate Commands</a>
     */
    @CheckReturnValue
    List<EventRecord> dispatch(Message command, CommandContext context) throws InvocationTargetException;

    /**
     * Returns the number of events until a next snapshot is made.
     *
     * @return a positive integer value
     */
    @CheckReturnValue
    int getSnapshotTrigger();
}
