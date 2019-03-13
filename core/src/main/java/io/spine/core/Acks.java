package io.spine.core;

import com.google.protobuf.Message;
import io.spine.protobuf.AnyPacker;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.TextFormat.shortDebugString;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Utilities for working with {@linkplain Ack acknowledgements}.
 */
public final class Acks {

    /** Prevents instantiation of this utility class. */
    private Acks() {
    }

    /**
     * Extracts the ID of the acknowledged command.
     *
     * @throws IllegalArgumentException
     *         if the acknowledgement does not contain a command ID
     */
    public static CommandId toCommandId(Ack ack) {
        checkNotNull(ack);
        Message unpacked = AnyPacker.unpack(ack.getMessageId());
        if (!(unpacked instanceof CommandId)) {
            throw newIllegalArgumentException(
                    "The acknowledgement does not contain a command ID: `%s`.",
                    shortDebugString(ack)
            );
        }
        CommandId commandId = (CommandId) unpacked;
        return commandId;
    }
}
