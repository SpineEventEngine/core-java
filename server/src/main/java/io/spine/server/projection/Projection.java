/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.projection;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.entity.EventPlayingEntity;
import io.spine.server.event.EventSubscriberMethod;
import io.spine.server.model.Model;
import io.spine.validate.ValidatingBuilder;

/**
 * {@link Projection} holds a structural representation of data extracted from a stream of events.
 *
 * <p>The process of projecting the event stream into data we collect is performed
 * by event subscribers for the events of interest. These event handlers are implemented
 * in the classes extending this abstract class.
 *
 * <p>Event subscribers are invoked by a {@link ProjectionRepository} that manages instances
 * of a stream projection class.
 *
 * @param <I> the type of the IDs
 * @param <M> the type of the state objects holding projection data
 */
public abstract class Projection<I,
                                 M extends Message,
                                 B extends ValidatingBuilder<M, ? extends Message.Builder>>
        extends EventPlayingEntity<I, M, B> {

    /**
     * Creates a new instance.
     *
     * @param id the ID for the new instance
     * @throws IllegalArgumentException if the ID is not of one of the supported types
     */
    protected Projection(I id) {
        super(id);
    }

    @Override
    protected ProjectionClass<?> thisClass() {
        return (ProjectionClass<?>) super.thisClass();
    }

    @Override
    protected ProjectionClass<?> getModelClass() {
        return Model.getInstance()
                    .asProjectionClass(getClass());
    }

    protected void handle(EventEnvelope event) {
        apply(event.getMessage(), event.getEventContext());
    }

    @Override
    @VisibleForTesting      // Overridden to expose this method to tests.
    protected B getBuilder() {
        return super.getBuilder();
    }

    @Override
    protected String getMissingTxMessage() {
        return "Projection modification is not available this way. " +
                "Please modify the state from an event subscribing method.";
    }

    /**
     * Plays events on the projection.
     *
     * <p>Unlike {@link Projection#play(Iterable)} this static method opens the
     * {@linkplain ProjectionTransaction transaction} before events are played, and closes it after.
     *
     * @return {@code true} if the projection state was changed as the result of playing the events
     */
    static boolean play(Projection projection, Iterable<Event> events) {
        final ProjectionTransaction tx = ProjectionTransaction.start(projection);
        projection.play(events);
        tx.commit();
        return projection.isChanged();
    }

    void apply(Message eventMessage, EventContext eventContext)  {
        final EventSubscriberMethod method = thisClass().getSubscriber(EventClass.of(eventMessage));
        method.invoke(this, eventMessage, eventContext);
    }
}
