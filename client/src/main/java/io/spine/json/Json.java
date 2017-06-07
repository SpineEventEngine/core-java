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

package io.spine.json;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.spine.type.KnownTypes;
import io.spine.type.TypeUrl;
import io.spine.type.UnknownTypeException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.getRootCause;
import static io.spine.protobuf.Messages.builderFor;
import static io.spine.util.Exceptions.newIllegalArgumentException;

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
        final String result = toJson(message, JsonPrinter.getInstance());
        return result;
    }

    /**
     * Converts the passed message into compact Json representation.
     *
     * <p>The resulted Json does not contain the line separators.
     *
     * @param message the {@code Message} object
     * @return the converted message to Json
     */
    public static String toCompactJson(Message message) {
        final JsonFormat.Printer compactPrinter = JsonPrinter.getInstance()
                                                             .omittingInsignificantWhitespace();
        final String result = toJson(message, compactPrinter);
        return result;
    }

    private static String toJson(Message message, JsonFormat.Printer printer) {
        checkNotNull(message);
        String result;
        try {
            result = printer.print(message);
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
            JsonParser.getInstance()
                      .merge(json, messageBuilder);
            final T result = (T) messageBuilder.build();
            return result;
        } catch (InvalidProtocolBufferException e) {
            throw newIllegalArgumentException(e,
                                              "%s cannot be parsed to the %s class.",
                                              json, messageClass);
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

        private static JsonFormat.Printer getInstance() {
            return INSTANCE.value;
        }
    }

    private enum JsonParser {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final JsonFormat.Parser value = JsonFormat.parser()
                                                          .usingTypeRegistry(typeRegistry);

        private static JsonFormat.Parser getInstance() {
            return INSTANCE.value;
        }
    }
}
