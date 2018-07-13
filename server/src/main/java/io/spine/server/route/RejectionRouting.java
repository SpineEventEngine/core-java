/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.route;

import com.google.common.base.Optional;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionContext;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A routing schema used by a {@link io.spine.server.rejection.RejectionDispatcher
 * RejectionDispatcher} for delivering rejections.
 *
 * <p>A routing schema consists of a default route and custom routes per rejection class.
 * When calculating a set of rejection targets, {@code RejectionRouting} would see if there
 * is a custom route set for the type of the rejection. If not found, the default route will be
 * {@linkplain RejectionRoute#apply(Message, Message) applied}.
 *
 * @param <I> the type of the entity IDs to which rejections are routed
 * @author Alexander Yevsyukov
 */
public final class RejectionRouting<I>
        extends MessageRouting<RejectionContext, RejectionClass, Set<I>>
        implements RejectionRoute<I, Message> {

    private static final long serialVersionUID = 0L;

    private RejectionRouting(Route<Message, RejectionContext, Set<I>> defaultRoute) {
        super(defaultRoute);
    }

    /**
     * Creates new instance with the passed default route.
     *
     * @param defaultRoute
     *        the route to use if a custom one is not {@linkplain #route(Class, RejectionRoute) set}
     */
    @CanIgnoreReturnValue
    public static <I> RejectionRouting<I> withDefault(RejectionRoute<I, Message> defaultRoute) {
        checkNotNull(defaultRoute);
        return new RejectionRouting<>(defaultRoute);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    public final RejectionRoute<I, Message> getDefault() {
        return (RejectionRoute<I, Message>) super.getDefault();
    }

    /**
     * Sets new default route in the schema.
     *
     * @param newDefault the new route to be used as default
     * @return {@code this} to allow chained calls when configuring the routing
     */
    @CanIgnoreReturnValue
    public RejectionRouting<I> replaceDefault(RejectionRoute<I, Message> newDefault) {
        return (RejectionRouting<I>) super.replaceDefault(newDefault);
    }

    /**
     * Sets a custom route for the passed rejection class.
     *
     * <p>Such a mapping may be required when...
     * <ul>
     * <li>An an rejection message should be matched to more than one entity (e.g. several
     * process managers updated in response to one rejection).
     * <li>The type of an rejection producer ID (stored in the context) differs from the type
     * of entity identifiers ({@code <I>}.
     * </ul>
     *
     * <p>If there is no specific route for the class of the passed rejection, the routing will use
     * the {@linkplain #getDefault() default route}.
     *
     * @param rejectionClass the class of rejections to route
     * @param via            the instance of the route to be used
     * @param <R>            the type of the rejection message
     * @return {@code this} to allow chained calls when configuring the routing
     * @throws IllegalStateException if the route for this rejection class is already set
     */
    @CanIgnoreReturnValue
    public <R extends Message> RejectionRouting<I> route(Class<R> rejectionClass,
                                                         RejectionRoute<I, R> via)
        throws IllegalStateException {
        @SuppressWarnings("unchecked") // The cast is required to adapt the type to internal API.
        Route<Message, RejectionContext, Set<I>> casted =
                (Route<Message, RejectionContext, Set<I>>) via;
        return (RejectionRouting<I>) doRoute(rejectionClass, casted);
    }

    /**
     * Obtains a route for the passed rejection class.
     *
     * @param rejectionClass the class of the rejection messages
     * @param <M>            the type of the rejection message
     * @return optionally available route
     */
    public <M extends Message> Optional<RejectionRoute<I, M>> get(Class<M> rejectionClass) {
        Optional<? extends Route<Message, RejectionContext, Set<I>>> optional =
                doGet(rejectionClass);
        if (optional.isPresent()) {
            RejectionRoute<I, M> route = (RejectionRoute<I, M>) optional.get();
            return Optional.of(route);
        }
        return Optional.absent();
    }

    @Override
    RejectionClass toMessageClass(Class<? extends Message> classOfMessages) {
        return RejectionClass.of(classOfMessages);
    }

    @Override
    RejectionClass toMessageClass(Message outerOrMessage) {
        return RejectionClass.of(outerOrMessage);
    }
}
