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
import com.google.protobuf.*;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.CommandId;
import org.spine3.base.EventId;
import org.spine3.protobuf.Messages;
import org.spine3.test.project.ProjectId;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.TimeUtil.getCurrentTime;

/**
 * A server-side wrapper for message objects with identity stored by a repository.
 *
 * @param <I> the type of object IDs
 * @param <S> the type of object states.
 */
public abstract class Entity<I, S extends Message> {

    public static final char ID_DELIMITER = '@';

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

    @SuppressWarnings({"IfMayBeConditional", "UnusedAssignment"})
    private static String idMessageToString(Message message) {

        final Map<Descriptors.FieldDescriptor, Object> fieldsMap = message.getAllFields();

        if (fieldsMap.isEmpty()) {
            return "";
        }

        String result = "";

        final IdConverterRegistry registry = IdConverterRegistry.instance();

        if (registry.containsConverter(message)) {
            final Function<Message, String> converter = registry.getConverter(message);
            result = converter.apply(message);
        } else {
            result = idMessageToStringManually(message, fieldsMap);
        }

        return result;
    }

    @SuppressWarnings({"TypeMayBeWeakened", "IfMayBeConditional"})
    private static String idMessageToStringManually(Message message, Map<Descriptors.FieldDescriptor, Object> fieldsMap) {

        String result;

        final Collection<Object> values = fieldsMap.values();

        if (values.size() == 1) {

            final Object object = values.iterator().next();

            if (object instanceof Message) {
                result = idMessageToString((Message) object);
            } else {
                result = object.toString();
            }

        } else {
            result = TextFormat.shortDebugString(message);
        }

        return result;
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
    @SuppressWarnings({"serial", "ClassExtendsConcreteCollection", "InnerClassTooDeeplyNested", "ConstantConditions",
            "StringBufferWithoutInitialCapacity"})
    public static class IdConverterRegistry {

        private final Map<Class<?>, Function<?, String>> entries = new HashMap<Class<?>, Function<?, String>>() {{
            put(CommandId.class, new CommandIdToStringConverter());
            put(EventId.class, new EventIdToStringConverter());
            put(ProjectId.class, new ProjectIdToStringConverter());
            put(Timestamp.class, new TimestampToStringConverter());
        }};

        private IdConverterRegistry() {
        }

        public <I extends Message> void register(Class<I> idClass, Function<I, String> converter) {
            checkNotNull(idClass);
            checkNotNull(converter);
            entries.put(idClass, converter);
        }

        public <I> Function<I, String> getConverter(I id) {
            final Function<?, String> func = entries.get(id.getClass());

            @SuppressWarnings("unchecked") /** The cast is safe as we check the first type when adding.
                @see #register(Class, Function) */
            final Function<I, String> result = (Function<I, String>) func;
            return result;
        }

        public <I> boolean containsConverter(I id) {
            final boolean contains = entries.containsKey(id.getClass()) && (entries.get(id.getClass()) != null);
            return contains;
        }

        private enum Singleton {
            INSTANCE;
            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final IdConverterRegistry value = new IdConverterRegistry();
        }

        public static IdConverterRegistry instance() {
            return Singleton.INSTANCE.value;
        }

        private static class CommandIdToStringConverter implements Function<CommandId, String> {
            @Nullable
            @Override
            public String apply(@Nullable CommandId commandId) {

                if (commandId == null) {
                    return "";
                }

                final StringBuilder builder = new StringBuilder();

                final String userId = userIdToString(commandId);
                final String commandTime = (commandId != null) ? TimeUtil.toString(commandId.getTimestamp()) : "";

                builder.append(userId)
                        .append(ID_DELIMITER)
                        .append(commandTime);

                return builder.toString();
            }
        }

        private static class EventIdToStringConverter implements Function<EventId, String> {
            @Nullable
            @Override
            public String apply(@Nullable EventId eventId) {

                if (eventId == null) {
                    return "";
                }

                final StringBuilder builder = new StringBuilder();

                final CommandId commandId = eventId.getCommandId();

                final String userId = userIdToString(commandId);
                final String commandTime = (commandId != null) ? TimeUtil.toString(commandId.getTimestamp()) : "";
                final String eventTime = (eventId != null) ? TimeUtil.toString(eventId.getTimestamp()) : "";

                builder.append(userId)
                        .append(ID_DELIMITER)
                        .append(commandTime)
                        .append(ID_DELIMITER)
                        .append(eventTime);

                return builder.toString();
            }
        }

        private static class ProjectIdToStringConverter implements Function<ProjectId, String> {
            @SuppressWarnings("IfMayBeConditional")
            @Nullable
            @Override
            public String apply(@Nullable ProjectId projectId) {
                if (projectId != null) {
                    return projectId.getId();
                } else {
                    return "";
                }
            }
        }

        private static class TimestampToStringConverter implements Function<Timestamp, String> {
            @SuppressWarnings("IfMayBeConditional")
            @Nullable
            @Override
            public String apply(@Nullable Timestamp timestamp) {
                if (timestamp != null) {
                    return TimeUtil.toString(timestamp);
                } else {
                    return "";
                }
            }
        }

        @SuppressWarnings("TypeMayBeWeakened")
        private static String userIdToString(CommandId commandId) {

            if (commandId == null || commandId.getActor() == null) {
                return "unknown";
            }
            final String userId = commandId.getActor().getValue();
            return userId;
        }
    }
}
