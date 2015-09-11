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

import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import org.spine3.CommandClass;
import org.spine3.base.CommandContext;
import org.spine3.base.EventRecord;
import org.spine3.server.RepositoryBase;
import org.spine3.server.RepositoryEventStore;
import org.spine3.server.Snapshot;
import org.spine3.server.internal.CommandHandler;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;

/**
 * Abstract base for aggregate root repositories.
 *
 * @param <R> the type of the aggregated root
 * @param <I> the type of the aggregated root id
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods") // we can not have instances of AbstractRepository.
public abstract class AggregateRootRepositoryBase<I extends Message,
                                                  R extends AggregateRoot<I, ?>>
        extends RepositoryBase<I, R> implements AggregateRootRepository<I, R> {

    private static final String DISPATCH_METHOD_NAME = "dispatch";

    private RepositoryEventStore eventStore;

    protected AggregateRootRepositoryBase() {
        super();
    }

    /**
     * Configures repository with passed implementation of the aggregate storage.
     * It is used for storing and loading aggregated root during handling
     * of the incoming commands.
     *
     * @param eventStore the event store implementation
     */
    public void configure(RepositoryEventStore eventStore) {
        this.eventStore = eventStore;
    }

    /**
     * Creates a map of handlers that call {@link #dispatch(Message, CommandContext)}
     * method for all commands of the aggregate root class.
     */
    public Map<CommandClass, CommandHandler> getCommandHandlers() {
        Map<CommandClass, CommandHandler> result = Maps.newHashMap();

        Class<? extends AggregateRoot> rootClass = TypeInfo.getEntityClass(this);
        Set<CommandClass> commandClasses = AggregateRoot.getCommandClasses(rootClass);

        CommandHandler handler = toCommandHandler();
        for (CommandClass commandClass : commandClasses) {
            result.put(commandClass, handler);
        }
        return result;
    }

    /**
     * Returns the reference to the method {@link #dispatch(Message, CommandContext)} of this repository.
     */
    private CommandHandler toCommandHandler() {
        try {
            Method method = getClass().getMethod(DISPATCH_METHOD_NAME, Message.class, CommandContext.class);
            final CommandHandler result = new CommandHandler(this, method);
            return result;
        } catch (NoSuchMethodException e) {
            throw propagate(e);
        }
    }

    /**
     * Loads the an aggregate by given id.
     *
     * @param id id of the aggregate to load
     * @return the loaded object
     * @throws IllegalStateException if the repository wasn't configured prior to calling this method
     */
    @Nonnull
    @Override
    public R load(I id) throws IllegalStateException {
        checkConfigured();

        try {
            Snapshot snapshot = eventStore.getLastSnapshot(id);
            if (snapshot != null) {
                List<EventRecord> trail = eventStore.getEvents(id, snapshot.getVersion());
                R result = create(id);
                result.restore(snapshot);
                result.play(trail);
                return result;
            } else {
                List<EventRecord> events = eventStore.getAllEvents(id);
                R result = create(id);
                result.play(events);
                return result;
            }
        } catch (InvocationTargetException e) {
            throw propagate(e);
        }
    }

    private void checkConfigured() {
        if (eventStore == null) {
            throw new IllegalStateException("Repository instance is not configured."
                    + "Call the configure() method before trying to load/save the aggregate root.");
        }
    }

    /**
     * Stores the passed aggregate root.
     *
     * @param aggregateRoot an instance to store
     */
    @Override
    public void store(R aggregateRoot) {
        checkConfigured();

        //TODO:2015-09-05:alexander.yevsyukov: Store snapshots every Xxx messages, which should be configured at the repository's level.

        Snapshot snapshot = aggregateRoot.toSnapshot();

        //noinspection unchecked
        final I aggregateRootId = aggregateRoot.getId();
        eventStore.storeSnapshot(aggregateRootId, snapshot);
    }

    @Override
    public List<EventRecord> dispatch(Message command, CommandContext context) throws InvocationTargetException {
        //TODO:2015-09-05:alexander.yevsyukov: Where do we handle a command processed by a repository's method?

        I aggregateId = getAggregateId(command);
        R aggregateRoot = load(aggregateId);

        aggregateRoot.dispatch(command, context);

        //noinspection unchecked
        final List<EventRecord> eventRecords = aggregateRoot.getUncommittedEvents();

        store(aggregateRoot);

        return eventRecords;
    }

    @SuppressWarnings("unchecked")
    // We cast to this type because assume that all commands for our aggregate refer to ID of the same type <I>.
    // If this assumption fails, we would get ClassCastException.
    // A better way would be to check all the aggregate commands for the presence of the ID field and
    // correctness of the type on compile-time.
    private I getAggregateId(Message command) {
        return (I) AggregateCommand.getAggregateId(command).value();
    }

}
