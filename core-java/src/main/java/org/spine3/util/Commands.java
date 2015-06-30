/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.util;

import com.google.common.base.Predicate;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandId;
import org.spine3.base.CommandRequest;
import org.spine3.base.UserId;
import org.spine3.lang.MissingAggregateIdException;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;

/**
 * Utility class for working with {@link CommandId} objects.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class Commands {

    public static final String ID_PROPERTY_SUFFIX = "id";

    /**
     * Creates a new {@link CommandId} taking passed {@link UserId} object and current system time.
     *
     * @param userId ID of the user who originates the command
     * @return new command ID
     */
    public static CommandId generateId(UserId userId) {
        checkNotNull(userId);

        return CommandId.newBuilder()
                .setActor(userId)
                .setTimestamp(Timestamps.now())
                .build();
    }

    /**
     * Obtains Protobuf field name for the passed command.
     *
     * @param command a command to inspect
     * @param index   a zero-based index of the field
     * @return name of the field
     */
    @SuppressWarnings("TypeMayBeWeakened") // Enforce type for API clarity.
    public static String getFieldName(Message command, int index) {
        final Descriptors.FieldDescriptor fieldDescriptor = checkNotNull(command).getDescriptorForType().getFields().get(index);
        String fieldName = fieldDescriptor.getName();
        return fieldName;
    }

    public static Message getAggregateId(Message command) {
        String fieldName = getFieldName(command, 0);
        if (!fieldName.endsWith(ID_PROPERTY_SUFFIX)) {
            throw new MissingAggregateIdException(command.getClass().getName(), fieldName);
        }

        try {
            Message result = (Message) getFieldValue(command, 0);
            return result;
        } catch (RuntimeException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new MissingAggregateIdException(command, toAccessorMethodName(fieldName), e.getCause());
        }
    }

    /**
     * Reads field from the passed command.
     *
     * @param command    a command to inspect
     * @param fieldIndex a zero-based index of the field
     * @return value a value of the field
     */
    @SuppressWarnings("TypeMayBeWeakened") // Enforce type for API clarity
    public static Object getFieldValue(Message command, int fieldIndex) {
        final Descriptors.FieldDescriptor fieldDescriptor = checkNotNull(command).getDescriptorForType().getFields().get(fieldIndex);
        String fieldName = fieldDescriptor.getName();
        String methodName = toAccessorMethodName(fieldName);

        try {
            Method method = command.getClass().getMethod(methodName);
            Object result = method.invoke(command);
            return result;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw propagate(e);
        }
    }

    /**
     * Converts Protobuf field name into Java accessor method name.
     */
    public static String toAccessorMethodName(CharSequence fieldName) {
        StringBuilder out = new StringBuilder(checkNotNull(fieldName).length() + 3);
        out.append("get");
        out.append(Character.toUpperCase(fieldName.charAt(0)));
        boolean nextUpperCase = false;
        for (int i = 1; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if ('_' == c) {
                nextUpperCase = true;
                continue;
            }
            out.append(nextUpperCase ? Character.toUpperCase(c) : c);
            nextUpperCase = false;
        }

        return out.toString();
    }

    /**
     * Obtains initial aggregate root state from the creation command.
     * <p>
     * The state must be the second field of the Protobuf message, and must match
     * the message type of the corresponding aggregated root state.
     *
     * @param creationCommand the command to inspect
     * @return initial aggregated root state
     * @throws IllegalStateException if the field value is not a {@link Message} instance
     */
    public static Message getAggregateState(Message creationCommand) {
        Object state = getFieldValue(creationCommand, 1);
        Message result;
        try {
            result = (Message) state;
        } catch (ClassCastException ignored) {
            throw new IllegalStateException("The second field of the aggregate creation command must be Protobuf message. Found: " + state.getClass());
        }
        return result;
    }

    private Commands() {
    }

    public static Predicate<CommandRequest> getCommandPredicate(final Timestamp from, final Timestamp to) {
        return new Predicate<CommandRequest>() {
            @Override
            public boolean apply(@Nullable CommandRequest request) {
                checkNotNull(request);
                Timestamp timestamp = request.getContext().getCommandId().getTimestamp();
                return Timestamps.isBetween(timestamp, from, to);
            }
        };
    }

    /**
     * Sorts the command given command request list by command timestamp value.
     *
     * @param commandRequests the command request list to sort
     */
    public static void sort(List<CommandRequest> commandRequests) {
        Collections.sort(commandRequests, new Comparator<CommandRequest>() {
            @Override
            public int compare(CommandRequest o1, CommandRequest o2) {
                Timestamp timestamp1 = o1.getContext().getCommandId().getTimestamp();
                Timestamp timestamp2 = o2.getContext().getCommandId().getTimestamp();
                return Timestamps.compare(timestamp1, timestamp2);
            }
        });
    }

}
