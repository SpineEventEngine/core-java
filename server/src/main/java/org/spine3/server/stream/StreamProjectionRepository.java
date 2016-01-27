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

package org.spine3.server.stream;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.EventContext;
import org.spine3.server.BoundedContext;
import org.spine3.server.EntityRepository;
import org.spine3.server.EventDispatcher;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.StreamProjectionStorage;
import org.spine3.server.util.Identifiers;
import org.spine3.type.EventClass;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Abstract base for repositories managing {@link StreamProjection}s.
 *
 * @author Alexander Yevsyukov
 */
public abstract class StreamProjectionRepository<I, P extends StreamProjection<I, M>, M extends Message>
        extends EntityRepository<I, P, M> implements EventDispatcher {

    protected StreamProjectionRepository(BoundedContext boundedContext) {
        super(boundedContext);
    }

    @Override
    @SuppressWarnings("RefusedBequest")
    protected AutoCloseable createStorage(StorageFactory factory) {
        return factory.createStreamProjectionStorage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<EventClass> getEventClasses() {
        final Class<? extends StreamProjection> projectionClass = getEntityClass();
        final Set<Class<? extends Message>> eventClasses = StreamProjection.getEventClasses(projectionClass);
        final Set<EventClass> result = EventClass.setOf(eventClasses);
        return result;
    }

    /**
     * Casts result of {@link EventContext#getAggregateId()} to the type of index of this
     * storage.
     *
     * <p>Override to provide custom logic of ID generation.
     */
    @SuppressWarnings("UnusedParameters") // Overriding methods may want to use this parameter.
    protected I getEntityId(Message event, EventContext context) {
        final Object aggregateId = Identifiers.idFromAny(context.getAggregateId());
        @SuppressWarnings("unchecked")
        final I id = (I)aggregateId;
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
     * Dispatches the passed event to corresponding {@link StreamProjection}.
     *
     * <p>The ID of the projection must be specified as the first property of the passed event.
     *
     * <p>If there is no stored projection with the ID from the event, a new projection is created
     * and stored after it handles the passed event.
     *
     * @param event the event to dispatch
     * @param context the context of the event
     * @see StreamProjection#handle(Message, EventContext)
     */
    @Override
    public void dispatch(Message event, EventContext context) {
        final I id = getEntityId(event, context);
        final P sp = load(id);
        sp.handle(event, context);
        store(sp);
        final StreamProjectionStorage<I> storage = streamProjectionStorage();
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
    protected StreamProjectionStorage<I> streamProjectionStorage() {
        @SuppressWarnings("unchecked") // It is safe to cast as we control the creation in createStorage().
        final StreamProjectionStorage<I> storage = (StreamProjectionStorage<I>) getStorage();
        return checkStorage(storage);
    }
}
