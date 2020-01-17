/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.core;

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.time.InstantConverter;
import io.spine.validate.FieldAwareMessage;

import java.time.Instant;

import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Mixin interface for {@link EventContext}s.
 */
@Immutable
interface EventContextMixin extends EnrichableMessageContext,
                                    EventContextOrBuilder,
                                    FieldAwareMessage {

    /**
     * Obtains an actor context for the event context.
     *
     * <p>The context is obtained by traversing the events origin for a valid context source.
     * There can be three sources for the actor context:
     * <ol>
     *     <li>The command context set as the event origin.
     *     <li>The command context of an event which is an origin of this event.
     *     <li>The import context if the event is imported to an aggregate.
     * </ol>
     *
     * <p>If at some point the event origin is not set, an {@link IllegalArgumentException} is
     * thrown as it contradicts the Spine validation rules. See {@link EventContext} proto
     * declaration.
     */
    @SuppressWarnings({
            "ClassReferencesSubclass", // which is the only impl.
            "deprecation" // For backward compatibility.
    })
    default ActorContext actorContext() {
        ActorContext actorContext = null;
        EventContext ctx = (EventContext) this;

        while (actorContext == null) {
            switch (ctx.getOriginCase()) {
                case COMMAND_CONTEXT:
                    actorContext = ctx.getCommandContext()
                                      .getActorContext();
                    break;
                case EVENT_CONTEXT:
                    ctx = ctx.getEventContext();
                    break;
                case PAST_MESSAGE:
                    actorContext = ctx.getPastMessage()
                                      .getActorContext();
                    break;
                case IMPORT_CONTEXT:
                    actorContext = ctx.getImportContext();
                    break;
                case ORIGIN_NOT_SET:
                default:
                    throw newIllegalStateException(
                            "The provided event context has no origin defined."
                    );
            }
        }
        return actorContext;
    }

    /**
     * Obtains the actor user ID.
     *
     * <p>The 'actor' is the user responsible for producing the given event.
     *
     * <p>It is obtained as follows:
     * <ul>
     *     <li>For the events generated from commands, the actor context is taken from the
     *         enclosing command context.
     *     <li>For the event react chain, the command context of the topmost event is used.
     *     <li>For the imported events, the separate import context contains information about an
     *         actor.
     * </ul>
     *
     * <p>If the given event context contains no origin, an {@link IllegalArgumentException} is
     * thrown as it contradicts the Spine validation rules.
     */
    default UserId actor() {
        return actorContext().getActor();
    }

    /**
     * Obtains the ID of the entity which produced the event.
     */
    default Object producer() {
        @SuppressWarnings("ClassReferencesSubclass")  // which is the only impl.
        EventContext thisContext = (EventContext) this;
        return Identifier.unpack(thisContext.getProducerId());
    }

    /**
     * Obtains the time of the event as {@link Timestamp}.
     *
     * @see #instant()
     */
    default Timestamp timestamp() {
        return getTimestamp();
    }

    /**
     * Obtains the time of the event as {@link Instant}.
     *
     * @see #timestamp()
     */
    default Instant instant() {
        Instant result = InstantConverter.reversed()
                                         .convert(getTimestamp());
        return result;
    }

    /**
     * Reads the values of the fields without using the reflection.
     *
     * <p>During the validation of the contents this call reduces the cost of value extraction.
     * It is needed to improve the performance. However, some of the fields in {@code EventContext}
     * are deprecated, so the respective warnings are suppressed.
     */
    @SuppressWarnings({"OverlyComplexMethod", "MagicNumber", "deprecation"})    // see the docs.
    @Override
    @Internal
    default Object readValue(Descriptors.FieldDescriptor field) {
        switch (field.getIndex()) {
            case 0:
                return getTimestamp();
            case 1:
                return getCommandContext();
            case 2:
                return getEventContext();
            case 3:
                return getPastMessage();
            case 4:
                return getImportContext();
            case 5:
                return getCommandId();
            case 6:
                return getEventId();
            case 7:
                return getRootCommandId();
            case 8:
                return getProducerId();
            case 9:
                return getVersion();
            case 10:
                return getEnrichment();
            case 11:
                return getExternal();
            case 12:
                return getRejection();
            default:
                return getField(field);
        }
    }
}
