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

package org.spine3.server;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.protobuf.*;
import org.spine3.protobuf.Messages;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Map;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.protobuf.Messages.toJson;

/**
 * A server-side wrapper for message objects with identity stored by a repository.
 *
 * @param <I> the type of object IDs
 * @param <S> the type of object states.
 */
public abstract class Entity<I, S extends Message> {

    private final I id;

    @Nullable
    private S state;

    @Nullable
    private Timestamp whenLastModified;

    private int version;

    protected Entity(I id) {
        this.id = id;
    }

    @CheckReturnValue
    protected abstract S getDefaultState();

    /**
     * @return the current state object or {@code null} if the state wasn't set
     */
    @CheckReturnValue
    @Nullable
    public S getState() {
        return state;
    }

    /**
     * Validates the passed state.
     * <p/>
     * Does nothing by default. Aggregate roots may override this method to
     * specify logic of validating initial or intermediate state of the root.
     *
     * @param state a state object to replace the current state
     * @throws IllegalStateException if the state is not valid
     */
    @SuppressWarnings({"NoopMethodInAbstractClass", "UnusedParameters"})
    // Have this no-op method to prevent enforcing implementation in all sub-classes.
    protected void validate(S state) throws IllegalStateException {
        // Do nothing by default.
    }

    /**
     * Validates and sets the state.
     *
     * @param state the state object to set
     * @see #validate(S)
     */
    protected void setState(S state, int version, Timestamp whenLastModified) {
        validate(state);
        this.state = state;
        this.version = version;
        this.whenLastModified = whenLastModified;
    }

    /**
     * Updates the state incrementing the version number and recording time of the modification
     *
     * @param newState a new state to set
     */
    protected void incrementState(S newState) {
        setState(newState, incrementVersion(), getCurrentTime());
    }

    /**
     * Sets the object into the default state.
     * <p/>
     * Results of this method call are:
     * <ul>
     * <li>The state object is set to the value produced by {@link #getDefaultState()}.</li>
     * <li>The version number is set to zero.</li>
     * <li>The timestamp is set to the system time of the call.</li>
     * </ul>
     * <p/>
     * The timestamp is set to current system time.
     */
    protected void setDefault() {
        setState(getDefaultState(), 0, getCurrentTime());
    }

    /**
     * @return current version number
     */
    @CheckReturnValue
    public int getVersion() {
        return version;
    }

    /**
     * Advances the current version by one and records the time of the modification.
     *
     * @return new version number
     */
    protected int incrementVersion() {
        ++version;
        whenLastModified = getCurrentTime();

        return version;
    }

    @CheckReturnValue
    public I getId() {
        return id;
    }

    /**
     * Obtains the timestamp of the last modification.
     *
     * @return the timestamp instance or {@code null} if the state wasn't set
     * @see #setState(Message, int, Timestamp)
     */
    @CheckReturnValue
    @Nullable
    public Timestamp whenLastModified() {
        return this.whenLastModified;
    }

    // Utilities for ID conversion
    //------------------------------------

    /**
     * Converts the passed ID value into the string representation.
     *
     * @param id  the value to convert
     * @param <I> the type of the ID
     * @return <ul>
     * <li>For classes implementing {@link Message} — Json form</li>
     * <li>For {@code String}, {@code Long}, {@code Integer} — the result of {@link Object#toString()}</li>
     * </ul>
     * @throws IllegalArgumentException if the passed type isn't one of the above
     */
    public static <I> String idToString(I id) {
        //noinspection ChainOfInstanceofChecks
        if (id instanceof String
                || id instanceof Integer
                || id instanceof Long) {
            String toString = id.toString();
            return toString;
        }

        if (id instanceof Message) {
            final String result = idMessageToString((Message) id);
            return result;
        }
        throw unsupportedIdType(id);
    }

    private static String idMessageToString(Message message) {

        //TODO:2015-09-18:alexander.yevsyukov: Extract value from the message and use it for output
        /**
         *
         * @see TextFormat#printFieldValue(Descriptors.FieldDescriptor, Object, Appendable)
         *      and
         * @see com.google.protobuf.util.JsonFormat.ParserImpl
         *
         * for ideas on how we can do it.
         *
         * The guidelines:
         *   -  If it's one field inside — use it for string output.
         *   -  If more than one field, use TextFormat output and compact the form. See TextFormat.shortDebugString()
         *   -  Add our types as well known with good output.
         *
         * Also
         *
         * @see org.spine3.server.Entity.IdConverterRegistry below.
         *
         * We may add our types right into it during the initialization.
         */

        String json = toJson(message);
        return json;
    }

    /**
     * Wraps the passed ID value into an instance of {@link Any}.
     * <p/>
     * <p>The passed value must be of one of the supported types listed below.
     * The type of the value wrapped into the returned instance is defined by the type
     * of the passed value:
     * <ul>
     * <li>For classes implementing {@link Message} — the value of the message itself</li>
     * <li>For {@code String} — {@link StringValue}</li>
     * <li>For {@code Long} — {@link UInt64Value}</li>
     * <li>For {@code Integer} — {@link UInt32Value}</li>
     * </ul>
     *
     * @param id  the value to wrap
     * @param <I> the type of the value
     * @return instance of {@link Any} with the passed value
     * @throws IllegalArgumentException if the passed value is not of the supported type
     */
    public static <I> Any idToAny(I id) {
        Any anyId;
        //noinspection IfStatementWithTooManyBranches,ChainOfInstanceofChecks
        if (id instanceof Message) {
            Message message = (Message) id;
            anyId = Messages.toAny(message);
        } else if (id instanceof String) {
            String s = (String) id;
            anyId = Messages.toAny(StringValue.newBuilder().setValue(s).build());
        } else if (id instanceof Integer) {
            Integer intValue = (Integer) id;
            anyId = Messages.toAny(UInt32Value.newBuilder().setValue(intValue).build());
        } else if (id instanceof Long) {
            Long longValue = (Long) id;
            anyId = Messages.toAny(UInt64Value.newBuilder().setValue(longValue).build());
        } else {
            throw unsupportedIdType(id);
        }
        return anyId;
    }

    /**
     * Extracts ID object from the passed {@link Any} instance.
     * <p/>
     * <p>Returned type depends on the type of the message wrapped into {@code Any}.
     *
     * @param idInAny the ID value wrapped into {@code Any}
     * @return <ul>
     * <li>{@code String} value if {@link StringValue} is unwrapped</li>
     * <li>{@code Integer} value if {@link UInt32Value} is unwrapped</li>
     * <li>{@code Long} value if {@link UInt64Value} is unwrapped</li>
     * <li>unwrapped {@code Message} instance if its type is none of the above</li>
     * </ul>
     */
    public static Object idFromAny(Any idInAny) {
        Message extracted = Messages.fromAny(idInAny);

        //noinspection ChainOfInstanceofChecks
        if (extracted instanceof StringValue) {
            StringValueOrBuilder stringValue = (StringValue) extracted;
            return stringValue.getValue();
        }
        if (extracted instanceof UInt32Value) {
            UInt32Value uInt32Value = (UInt32Value) extracted;
            return uInt32Value.getValue();
        }
        if (extracted instanceof UInt64Value) {
            UInt64Value uInt64Value = (UInt64Value) extracted;
            return uInt64Value.getValue();
        }
        return extracted;
    }

    private static <I> IllegalArgumentException unsupportedIdType(I id) {
        return new IllegalArgumentException("ID of unsupported type encountered: " + id);
    }

    /**
     * The registry of converters of ID types to string representations.
     */
    public static class IdConverterRegistry {

        private final Map<Class<?>, Function<?, String>> entries = Maps.newHashMap();

        private IdConverterRegistry() {
        }

        public <I extends Message> void register(Class<I> idClass, Function<I, String> converter) {
            entries.put(idClass, converter);
        }

        @Nullable
        public <I> Function<I, String> getConverter(I id) {
            final Function<?, String> func = entries.get(id.getClass());

            @SuppressWarnings("unchecked") /** The cast is safe as we check the first type when adding.
                @see #register(Class, Function) */
            final Function<I, String> result = (Function<I, String>) func;
            return result;
        }

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final IdConverterRegistry value = new IdConverterRegistry();
        }

        public static IdConverterRegistry instance() {
            return Singleton.INSTANCE.value;
        }
    }
}
