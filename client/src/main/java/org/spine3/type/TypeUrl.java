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
import com.google.protobuf.Any;
import com.google.protobuf.AnyOrBuilder;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.GenericDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.spine3.Internal;
import org.spine3.annotations.AnnotationsProto;
import org.spine3.base.Command;
import org.spine3.base.Event;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.envelope.EventEnvelope;
import org.spine3.envelope.MessageEnvelope;

import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
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
 * @see Descriptors.FileDescriptor#getFullName()
 */
public final class TypeUrl extends StringTypeValue {

    private static final String SEPARATOR = "/";
    private static final Pattern TYPE_URL_SEPARATOR_PATTERN = Pattern.compile(SEPARATOR);

    private static final String PROTOBUF_PACKAGE_SEPARATOR = ".";
    private static final Pattern PROTOBUF_PACKAGE_SEPARATOR_PATTERN =
            Pattern.compile('\\' + PROTOBUF_PACKAGE_SEPARATOR);

    @VisibleForTesting
    static final String GOOGLE_TYPE_URL_PREFIX = "type.googleapis.com";

    public static final String SPINE_TYPE_URL_PREFIX = "type.spine3.org";

    private static final String GOOGLE_PROTOBUF_PACKAGE = "google.protobuf";

    /** The prefix of the type URL. */
    private final String prefix;

    /** The name of the Protobuf type. */
    private final String typeName;

    private TypeUrl(String prefix, String typeName) {
        super(composeTypeUrl(prefix, typeName));
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
    static String composeTypeUrl(String typeUrlPrefix, String typeName) {
        final String url = typeUrlPrefix + SEPARATOR + typeName;
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
     * Creates a new instance from the passed type URL or type name.
     *
     * @param typeUrlOrName the type URL of the Protobuf message type or its fully-qualified name
     */
    @Internal
    public static TypeUrl of(String typeUrlOrName) {
        checkNotEmptyOrBlank(typeUrlOrName, "type URL or name");
        final TypeUrl typeUrl = isTypeUrl(typeUrlOrName)
                                ? ofTypeUrl(typeUrlOrName)
                                : ofTypeName(typeUrlOrName);
        return typeUrl;
    }

    private static boolean isTypeUrl(String str) {
        return str.contains(SEPARATOR);
    }

    private static TypeUrl ofTypeUrl(String typeUrl) {
        final String[] parts = TYPE_URL_SEPARATOR_PATTERN.split(typeUrl);
        if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
            throw new IllegalArgumentException(
                    new InvalidProtocolBufferException("Invalid Protobuf type url encountered: " + typeUrl));
        }
        return create(parts[0], parts[1]);
    }

    private static TypeUrl ofTypeName(String typeName) {
        final TypeUrl typeUrl = KnownTypes.getTypeUrl(typeName);
        return typeUrl;
    }

    /**
     * Obtains the type URL of the message enclosed into the instance of {@link Any}.
     *
     * @param any the instance of {@code Any} containing a {@code Message} instance of interest
     * @return a type URL
     */
    public static TypeUrl ofEnclosed(AnyOrBuilder any) {
        final TypeUrl typeUrl = ofTypeUrl(any.getTypeUrl());
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
    public static TypeUrl of(Class<? extends Message> clazz) {
        final Message defaultInstance = com.google.protobuf.Internal.getDefaultInstance(clazz);
        final TypeUrl result = of(defaultInstance);
        return result;
    }

    private static String getTypeUrlPrefix(GenericDescriptor descriptor) {
        final Descriptors.FileDescriptor file = descriptor.getFile();
        if (file.getPackage().equals(GOOGLE_PROTOBUF_PACKAGE)) {
            return GOOGLE_TYPE_URL_PREFIX;
        }
        final String result = file.getOptions()
                                  .getExtension(AnnotationsProto.typeUrlPrefix);
        return result;
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

    /**
     * Returns the unqualified name of the Protobuf type, for example: {@code StringValue}.
     */
    public String getSimpleName() {
        if (typeName.contains(PROTOBUF_PACKAGE_SEPARATOR)) {
            final String[] parts = PROTOBUF_PACKAGE_SEPARATOR_PATTERN.split(typeName);
            checkState(parts.length > 0, "Invalid type name: " + typeName);
            final String result = parts[parts.length - 1];
            return result;
        } else {
            return typeName;
        }
    }

    /**
     * Retrieves all the type URLs that belong to the given package or its subpackages.
     *
     * @param packageName proto package name
     * @return set of {@link TypeUrl TypeUrl}s of types that belong to the given package
     */
    public static Set<TypeUrl> getAllFromPackage(String packageName) {
        return KnownTypes.getTypesFromPackage(packageName);
    }

    /**
     * Obtains all type URLs known to the application.
     */
    public static Set<TypeUrl> getAll() {
        return KnownTypes.getTypeUrls();
    }
}
