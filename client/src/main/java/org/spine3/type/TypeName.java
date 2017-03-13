/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.type;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.Event;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Utility class for obtaining a type name from a {@code Message}.
 *
 * @author Alexander Yevsyukov
 */
public class TypeName extends StringTypeValue {

    private static final String PROTOBUF_PACKAGE_SEPARATOR = ".";
    private static final Pattern PROTOBUF_PACKAGE_SEPARATOR_PATTERN =
            Pattern.compile('\\' + PROTOBUF_PACKAGE_SEPARATOR);

    private TypeName(String value) {
        super(value);
    }

    private static TypeName create(String value) {
        return new TypeName(value);
    }

    /**
     * Creates instance from the passed type URL.
     */
    public static TypeName from(TypeUrl typeUrl) {
        checkNotNull(typeUrl);
        return create(typeUrl.getTypeName());
    }

    /**
     * Obtains type name for the passed message.
     */
    public static TypeName of(Message message) {
        checkNotNull(message);
        return from(TypeUrl.of(message));
    }

    /**
     * Obtains type name for the passed message class.
     */
    public static TypeName of(Class<? extends Message> cls) {
        checkNotNull(cls);
        return from(TypeUrl.of(cls));
    }

    /**
     * Obtains type name from the message of the passed command.
     */
    public static TypeName ofCommand(Command command) {
        checkNotNull(command);
        return from(TypeUrl.ofCommand(command));
    }

    /**
     * Obtains type name from the message of the passed event.
     */
    public static TypeName ofEvent(Event event) {
        checkNotNull(event);
        return from(TypeUrl.ofEvent(event));
    }

    /**
     * Obtains type name for the message type by its descriptor.
     */
    public static TypeName from(Descriptor descriptor) {
        checkNotNull(descriptor);
        return from(TypeUrl.from(descriptor));
    }

    /**
     * Returns the unqualified name of the Protobuf type, for example: {@code StringValue}.
     */
    public String getSimpleName() {
        final String typeName = value();
        if (typeName.contains(PROTOBUF_PACKAGE_SEPARATOR)) {
            final String[] parts = PROTOBUF_PACKAGE_SEPARATOR_PATTERN.split(typeName);
            checkState(parts.length > 0, "Invalid type name: " + typeName);
            final String result = parts[parts.length - 1];
            return result;
        } else {
            return typeName;
        }
    }
}
