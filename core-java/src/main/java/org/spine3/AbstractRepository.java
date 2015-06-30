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
import org.spine3.base.Snapshot;
import org.spine3.engine.Media;
import org.spine3.util.Commands;
import org.spine3.util.Messages;
import org.spine3.util.Methods;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static com.google.common.base.Throwables.propagate;

/**
 * Abstract base for aggregate root repositories.
 *
 * @param <R> the type of the aggregated root
 * @param <I> the type of the aggregated root id
 * @param <C> the type of the command to create aggregate root instance
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public abstract class AbstractRepository<I extends Message,
                                         R extends AggregateRoot,
                                         C extends Message> implements Repository<I, R, C> {

    public static final String REPOSITORY_NOT_CONFIGURED = "Repository instance is not configured."
            + "Call the configure() method before trying to load/save the aggregate root.";
    private Media media;

    /**
     * Configures repository with passed implementation of the aggregate media.
     * It is used for storing and loading aggregated root during handling
     * of the incoming commands.
     *
     * @param media the aggregate media implementation
     */
    public void configure(Media media) {
        this.media = media;
    }

    /**
     * Loads the an aggregate by given id.
     *
     * @param aggregateId id of the aggregate to load
     * @return the loaded object
     */
    @Override
    public R load(I aggregateId) throws IllegalStateException {
        if (media == null) {
            throw new IllegalStateException(REPOSITORY_NOT_CONFIGURED);
        }

        try {
            Snapshot snapshot = media.readLastSnapshot(aggregateId);
            if (snapshot != null) {
                List<EventRecord> trail = media.readEvents(snapshot.getVersion());
                R result = create(aggregateId);
                result.restore(snapshot);
                result.play(trail);
                return result;
            } else {
                List<EventRecord> events = media.readEvents(aggregateId);
                R result = create(aggregateId);
                result.play(events);
                return result;
            }
        } catch (InvocationTargetException e) {
            throw propagate(e);
        }
    }

    /**
     * Stores the passed aggregate root.
     *
     * @param aggregateRoot an instance to store
     */
    @Override
    public void store(R aggregateRoot) {
        if (media == null) {
            throw new IllegalStateException(REPOSITORY_NOT_CONFIGURED);
        }

        Snapshot snapshot = Snapshot.newBuilder()
                .setState(Messages.toAny(aggregateRoot.getState()))
                .setVersion(aggregateRoot.getVersion())
                .setWhenLastModified(aggregateRoot.whenLastModified())
                .build();
        media.writeSnapshot(aggregateRoot.getId(), snapshot);
    }

    @Override
    public List<EventRecord> dispatch(Message command, CommandContext context) throws InvocationTargetException {
        I aggregateId = getAggregateId(command);
        R aggregateRoot = load(aggregateId);

        aggregateRoot.dispatch(command, context);

        //noinspection unchecked
        final List<EventRecord> eventRecords = aggregateRoot.getUncommittedEvents();

        //TODO:2015-06-24:mikhail.melnik: possibly we do not need to store every state change.
        store(aggregateRoot);

        return eventRecords;
    }

    /**
     * Creates, initializes, and stores a new aggregated root.
     * <p>
     * The initial state of the aggregate root is taken from the creation command.
     *
     * @param command creation command
     * @param context creation command context
     * @return a list of the event records
     * @throws InvocationTargetException if an exception occurs during command handling
     */
    @Subscribe
    @Override
    public List<EventRecord> handleCreate(C command, CommandContext context) throws InvocationTargetException {
        I id = getAggregateId(command);

        R aggregateRoot = create(id);

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
        return (I) Commands.getAggregateId(command);
    }

    /**
     * Returns a new instance of the aggregate root object
     * with id equals to passed id parameter and all field values set by default,
     *
     * @param id the id of the aggregate root to be created
     * @return the aggregate root instance
     */
    protected R create(I id) {
        try {
            R result = aggregateRootConstructor.newInstance(id);

            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw propagate(e);
        }
    }

    private final Constructor<R> aggregateRootConstructor;

    @SuppressWarnings("ThisEscapedInObjectConstruction") // as we need 'this' to get the runtime generic type values
    protected AbstractRepository() {
        try {
            Class<R> rootClass = Methods.getRepositoryAggregateRootClass(this);
            Class<I> idClass = Methods.getRepositoryAggregateIdClass(this);

            aggregateRootConstructor = rootClass.getConstructor(idClass);
        } catch (NoSuchMethodException e) {
            //noinspection ProhibitedExceptionThrown // this exception cannot occur, otherwise it is a fatal error
            throw new Error(e);
        }
    }

}
