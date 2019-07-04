/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.command;

import com.google.protobuf.Any;
import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.protobuf.TypeConverter;
import io.spine.server.command.model.CommandHandlerClass;
import io.spine.server.command.model.CommandHandlerMethod;
import io.spine.server.command.model.CommandHandlerMethod.Result;
import io.spine.server.event.EventBus;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Suppliers.memoize;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.command.model.CommandHandlerClass.asCommandHandlerClass;

/**
 * The abstract base for non-aggregate classes that expose command handling methods
 * and post their results to {@link EventBus}.
 *
 * <p>A command handler is responsible for:
 * <ol>
 *   <li>Changing the state of the business model in response to a command.
 *   <li>Producing corresponding events.
 *   <li>Posting events to {@code EventBus}.
 * </ol>
 *
 * <p>Event messages are returned as values of {@linkplain Assign command handling methods}.
 *
 * <p>A command handler does not have own state. So the state of the business
 * model it changes is external to it. Even though such behaviour may be needed in
 * some rare cases, using {@linkplain io.spine.server.aggregate.Aggregate aggregates}
 * is a preferred way of handling commands.
 *
 * @implNote This class implements {@code CommandDispatcher} for dispatching messages
 *         to methods declared in the derived classes.
 * @see Assign @Assign
 * @see io.spine.server.aggregate.Aggregate Aggregate
 */
public abstract class AbstractCommandHandler
        extends AbstractCommandDispatcher
        implements CommandHandler {

    private final CommandHandlerClass<?> thisClass = asCommandHandlerClass(getClass());

    /** The bus to which post events. */
    private EventBus eventBus;

    /** Supplier for a packed version of the ID. */
    private final Supplier<Any> producerId =
            memoize(() -> pack(TypeConverter.toMessage(id())));

    /**
     * Dispatches the command to the handler method and
     * posts resulting events to the {@link EventBus}.
     *
     * @param envelope the command to dispatch
     * @return the handler identity as the result of {@link #toString()}
     * @throws IllegalStateException
     *         if an exception occurred during command dispatching with this exception as the cause
     */
    @Override
    public String dispatch(CommandEnvelope envelope) {
        CommandHandlerMethod method = thisClass.handlerOf(envelope.messageClass());
        Result result = method.invoke(this, envelope);
        List<Event> events = result.produceEvents(envelope);
        postEvents(events);
        return id();
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK as we return immutable impl.
    @Override
    public Set<CommandClass> messageClasses() {
        return thisClass.commands();
    }

    /**
     * Always returns {@linkplain Version#getDefaultInstance() empty} version.
     */
    @Override
    public Version version() {
        return Version.getDefaultInstance();
    }

    /**
     * Obtains {@linkplain #id() ID} packed into {@code Any} for being used in generated events.
     */
    @Override
    public Any producerId() {
        return producerId.get();
    }

    /**
     * Assigns {@code EventBus} to which the handler posts the produced events.
     */
    @Internal
    public final void injectEventBus(EventBus eventBus) {
        this.eventBus = checkNotNull(eventBus);
    }

    private EventBus eventBus() {
        return checkNotNull(eventBus, "`%s` does not have `EventBus` assigned.", this);
    }

    /**
     * Posts passed events to the associated {@code EventBus}.
     */
    private void postEvents(Iterable<Event> events) {
        eventBus().post(events);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Logs the error into the {@linkplain #log() log}.
     */
    @Override
    public void onError(CommandEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);
        _error(exception,
               "Error handling command (class: `{}` id: `{}`).",
               envelope.messageClass(),
               envelope.idAsString());
    }
}
