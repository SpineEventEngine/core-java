/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.projection;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.server.BoundedContext;
import org.spine3.server.entity.EntityRepository;
import org.spine3.server.event.EventDispatcher;
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.ProjectionStorage;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.type.EventClass;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Abstract base for repositories managing {@link Projection}s.
 *
 * @author Alexander Yevsyukov
 */
public abstract class ProjectionRepository<I, P extends Projection<I, M>, M extends Message>
        extends EntityRepository<I, P, M> implements EventDispatcher {

    private EntityStorage<I> entityStorage;

    protected ProjectionRepository(BoundedContext boundedContext) {
        super(boundedContext);
    }

    @Override
    @SuppressWarnings("RefusedBequest")
    protected AutoCloseable createStorage(StorageFactory factory) {
        final Class<P> projectionClass = getEntityClass();
        final ProjectionStorage<I> projectionStorage = factory.createProjectionStorage(projectionClass);
        this.entityStorage = projectionStorage.getEntityStorage();
        return projectionStorage;
    }

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException if the storage is null
     */
    @Override
    @Nonnull
    @SuppressWarnings("RefusedBequest")
    protected EntityStorage<I> entityStorage() {
        return checkStorage(entityStorage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<EventClass> getEventClasses() {
        final Class<? extends Projection> projectionClass = getEntityClass();
        final Set<Class<? extends Message>> eventClasses = Projection.getEventClasses(projectionClass);
        final Set<EventClass> result = EventClass.setOf(eventClasses);
        return result;
    }

    /**
     * Obtains the ID of the event producer from the passed event context and
     * casts it to the type of index used by this repository.
     *
     * @param event the event message. This parameter is not used by default implementation.
     *              Override to provide custom logic of ID generation.
     * @param context the event context
     */
    @SuppressWarnings("UnusedParameters") // Overriding methods may want to use the `event` parameter.
    protected I getEntityId(Message event, EventContext context) {
        final I id = Events.getProducer(context);
        return id;
    }

    /**
     * Loads or creates a projection by the passed ID.
     *
     * <p>The projection is created if there was no projection with such ID stored before.
     *
     * @param id the ID of the projection to load
     * @return loaded or created projection instance
     */
    @Nonnull
    @Override
    public P load(I id) {
        P result = super.load(id);
        if (result == null) {
            result = create(id);
        }
        return result;
    }

    /**
     * Dispatches the passed event to corresponding {@link Projection}.
     *
     * <p>The ID of the projection must be specified as the first property of the passed event.
     *
     * <p>If there is no stored projection with the ID from the event, a new projection is created
     * and stored after it handles the passed event.
     *
     * @param event the event to dispatch
     * @see Projection#handle(Message, EventContext)
     */
    @Override
    public void dispatch(Event event) {
        final Message eventMessage = Events.getMessage(event);
        final EventContext context = event.getContext();
        final I id = getEntityId(eventMessage, context);
        final P sp = load(id);
        sp.handle(eventMessage, context);
        store(sp);
        final ProjectionStorage<I> storage = projectionStorage();
        final Timestamp eventTime = context.getTimestamp();
        storage.writeLastHandledEventTime(eventTime);
    }

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException if the storage is null
     */
    @Nonnull
    protected ProjectionStorage<I> projectionStorage() {
        @SuppressWarnings("unchecked") // It is safe to cast as we control the creation in createStorage().
        final ProjectionStorage<I> storage = (ProjectionStorage<I>) getStorage();
        return checkStorage(storage);
    }

    public void catchUp() {
        //TODO:2016-05-02:alexander.yevsyukov: Implement
    }
}
