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

package org.spine3.json;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.spine3.type.KnownTypes;
import org.spine3.type.TypeUrl;
import org.spine3.type.UnknownTypeException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.getRootCause;
import static java.lang.String.format;
import static org.spine3.protobuf.Messages.builderFor;
import static org.spine3.util.Exceptions.newIllegalArgumentException;

/**
 * Utilities for working with Json.
 *
 * @author Alexander Yevsyukov
 */
public class Json {

    private static final JsonFormat.TypeRegistry typeRegistry = buildForKnownTypes();

    private Json() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Converts passed message into Json representation.
     *
     * @param message the message object
     * @return Json string
     */
    public static String toJson(Message message) {
        checkNotNull(message);
        String result;
        try {
            result = JsonPrinter.instance()
                                .print(message);
        } catch (InvalidProtocolBufferException e) {
            final Throwable rootCause = getRootCause(e);
            throw new UnknownTypeException(rootCause);
        }
        checkState(result != null);
        return result;
    }

    @SuppressWarnings("unchecked") // It is OK as the builder is obtained by the specified class.
    public static <T extends Message> T fromJson(String json, Class<T> messageClass) {
        checkNotNull(json);
        try {
            final Message.Builder messageBuilder = builderFor(messageClass);
            JsonParser.instance()
                      .merge(json, messageBuilder);
            final T result = (T) messageBuilder.build();
            return result;
        } catch (InvalidProtocolBufferException e) {
            final String exMessage = format("%s cannot be parsed to the %s class",
                                            json, messageClass);
            throw newIllegalArgumentException(exMessage, e);
        }
    }

    /**
     * Builds the registry of types known in the application.
     */
    private static JsonFormat.TypeRegistry buildForKnownTypes() {
        final JsonFormat.TypeRegistry.Builder builder = JsonFormat.TypeRegistry.newBuilder();
        for (TypeUrl typeUrl : KnownTypes.getAllUrls()) {
            final Descriptors.GenericDescriptor genericDescriptor = typeUrl.getDescriptor();
            if (genericDescriptor instanceof Descriptor) {
                final Descriptor descriptor = (Descriptor) genericDescriptor;
                builder.add(descriptor);
            }
        }
        return builder.build();
    }

    static JsonFormat.TypeRegistry typeRegistry() {
        return typeRegistry;
    }

    private enum JsonPrinter {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final JsonFormat.Printer value = JsonFormat.printer()
                                                           .usingTypeRegistry(typeRegistry);

        private static JsonFormat.Printer instance() {
            return INSTANCE.value;
        }
    }

    private enum JsonParser {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final JsonFormat.Parser value = JsonFormat.parser()
                                                          .usingTypeRegistry(typeRegistry);

        private static JsonFormat.Parser instance() {
            return INSTANCE.value;
        }
    }
}
