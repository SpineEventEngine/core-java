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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.protobuf.Any;
import com.google.protobuf.AnyOrBuilder;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.GenericDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.spine3.annotations.AnnotationsProto;
import org.spine3.annotations.Internal;
import org.spine3.base.Command;
import org.spine3.base.Event;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.envelope.EventEnvelope;
import org.spine3.envelope.MessageEnvelope;
import org.spine3.type.error.UnknownTypeException;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.Internal.getDefaultInstance;
import static org.spine3.validate.Validate.checkNotEmptyOrBlank;

/**
 * A URL of a Protobuf type.
 *
 * <p>Consists of the two parts separated with a slash.
 * The first part is the type URL prefix (for example, {@code "type.googleapis.com"});
 * the second part is a fully-qualified Protobuf type name.
 *
 * @author Alexander Yevsyukov
 * @see Any#getTypeUrl()
 * @see Descriptors.Descriptor#getFullName()
 */
public final class TypeUrl {

    private static final String SEPARATOR = "/";
    private static final Splitter splitter = Splitter.on(SEPARATOR);

    private static final String GOOGLE_PROTOBUF_PACKAGE = "google.protobuf";

    /** The prefix of the type URL. */
    private final String prefix;

    /** The name of the Protobuf type. */
    private final String typeName;

    private TypeUrl(String prefix, String typeName) {
        this.prefix = checkNotEmptyOrBlank(prefix, "typeUrlPrefix");
        this.typeName = checkNotEmptyOrBlank(typeName, "typeName");
    }

    /**
     * Create new {@code TypeUrl}.
     */
    private static TypeUrl create(String prefix, String typeName) {
        return new TypeUrl(prefix, typeName);
    }

    @VisibleForTesting
    static String composeTypeUrl(String prefix, String typeName) {
        final String url = prefix + SEPARATOR + typeName;
        return url;
    }

    /**
     * Creates a new type URL taking it from the passed message instance.
     *
     * @param msg an instance to get the type URL from
     */
    public static TypeUrl of(Message msg) {
        return from(msg.getDescriptorForType());
    }

    /**
     * Creates a new instance by the passed message descriptor taking its type URL.
     *
     * @param descriptor the descriptor of the type
     */
    public static TypeUrl from(Descriptor descriptor) {
        final String typeUrlPrefix = getTypeUrlPrefix(descriptor);
        return create(typeUrlPrefix, descriptor.getFullName());
    }

    /**
     * Creates a new instance by the passed enum descriptor taking its type URL.
     *
     * @param descriptor the descriptor of the type
     */
    public static TypeUrl from(EnumDescriptor descriptor) {
        final String typeUrlPrefix = getTypeUrlPrefix(descriptor);
        return create(typeUrlPrefix, descriptor.getFullName());
    }

    /**
     * Creates a new instance from the passed type URL.
     *
     * @param typeUrl the type URL of the Protobuf message type
     */
    @Internal
    public static TypeUrl parse(String typeUrl) {
        checkNotNull(typeUrl);
        checkArgument(!typeUrl.isEmpty());
        checkArgument(isTypeUrl(typeUrl), "Malformed type URL: %s", typeUrl);

        final TypeUrl result = doParse(typeUrl);
        return result;
    }

    private static boolean isTypeUrl(String str) {
        return str.contains(SEPARATOR);
    }

    private static TypeUrl doParse(String typeUrl) {
        final List<String> strings = splitter.splitToList(typeUrl);
        if (strings.size() != 2) {
            throw malformedTypeUrl(typeUrl);
        }
        final String prefix = strings.get(0);
        final String typeName = strings.get(1);
        return create(prefix, typeName);
    }

    private static IllegalArgumentException malformedTypeUrl(String typeUrl) {
        throw new IllegalArgumentException(
                new InvalidProtocolBufferException("Invalid Protobuf type URL encountered: "
                                                   + typeUrl));
    }

    /**
     * Obtains the type URL of the message enclosed into the instance of {@link Any}.
     *
     * @param any the instance of {@code Any} containing a {@code Message} instance of interest
     * @return a type URL
     */
    public static TypeUrl ofEnclosed(AnyOrBuilder any) {
        final TypeUrl typeUrl = doParse(any.getTypeUrl());
        return typeUrl;
    }

    /**
     * Obtains the type URL of the command message.
     *
     * <p>The passed command must have non-default message.
     *
     * @param command the command from which to get the URL
     * @return the type URL of the command message
     */
    public static TypeUrl ofCommand(Command command) {
        return ofEnclosedMessage(CommandEnvelope.of(command));
    }

    /**
     * Obtains the type URL of the event message.
     *
     * <p>The event must contain non-default message.
     *
     * @param event the event for which get the type URL
     * @return the type URL of the event message.
     */
    public static TypeUrl ofEvent(Event event) {
        return ofEnclosedMessage(EventEnvelope.of(event));
    }

    private static TypeUrl ofEnclosedMessage(MessageEnvelope envelope) {
        checkNotNull(envelope);
        final Message message = envelope.getMessage();
        final TypeUrl result = of(message);
        return result;
    }

    /** Obtains the type URL for the passed message class. */
    public static TypeUrl of(Class<? extends Message> cls) {
        final Message defaultInstance = getDefaultInstance(cls);
        final TypeUrl result = of(defaultInstance);
        return result;
    }

    private static String getTypeUrlPrefix(GenericDescriptor descriptor) {
        final Descriptors.FileDescriptor file = descriptor.getFile();
        if (file.getPackage().equals(GOOGLE_PROTOBUF_PACKAGE)) {
            return Prefix.GOOGLE_APIS.value();
        }
        final String result = file.getOptions()
                                  .getExtension(AnnotationsProto.typeUrlPrefix);
        return result;
    }

    /**
     * Returns a message {@link Class} corresponding to the Protobuf type represented
     * by this type URL.
     *
     * @return the message class
     * @throws UnknownTypeException wrapping {@link ClassNotFoundException} if
     *         there is no corresponding Java class
     */
    public <T extends Message> Class<T> getJavaClass() throws UnknownTypeException {
        return KnownTypes.getJavaClass(this);
    }

    /**
     * Obtains the type descriptor for the type of this URL.
     *
     * @return {@code Descriptor} or {@code Optional.absent()} if this type URL
     *         represents an outer class
     */
    @Internal
    public Optional<Descriptor> getTypeDescriptor() {
        final Class<? extends Message> clazz = getJavaClass();
        final GenericDescriptor descriptor = DescriptorUtil.getClassDescriptor(clazz);
        // Skip outer class descriptors.
        if (descriptor instanceof Descriptor) {
            final Descriptor typeDescriptor = (Descriptor) descriptor;
            return Optional.of(typeDescriptor);
        }
        return Optional.absent();
    }

    /**
     * Obtains the prefix of the type URL.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Obtains the type name.
     */
    public String getTypeName() {
        return typeName;
    }

    @Override
    public String toString() {
        return value();
    }

    public String value() {
        final String result = composeTypeUrl(prefix, typeName);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TypeUrl)) {
            return false;
        }
        TypeUrl typeUrl = (TypeUrl) o;
        return Objects.equals(prefix, typeUrl.prefix) &&
               Objects.equals(typeName, typeUrl.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, typeName);
    }

    /**
     * Enumeration of known type URL prefixes.
     */
    public enum Prefix {

        /**
         * Type prefix for standard Protobuf types.
         */
        GOOGLE_APIS("type.googleapis.com"),

        /**
         * Type prefix for types provided by the Spine framework.
         */
        SPINE("type.spine3.org");

        private final String value;

        Prefix(String value) {
            this.value = value;
        }

        /**
         * Obtains the value of the prefix.
         */
        public String value() {
            return value;
        }

        /**
         * Returns the value of the prefix.
         */
        @Override
        public String toString() {
            return value();
        }
    }
}
