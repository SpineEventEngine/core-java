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

package org.spine3.server.aggregate;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.protobuf.Messages;
import org.spine3.server.Entity;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Value object for aggregate IDs.
 *
 * <p>An aggregate ID value can be of one of the following types:
 *   <ul>
 *      <li>String</li>
 *      <li>Long</li>
 *      <li>Integer</li>
 *      <li>A class implementing {@link Message}</li>
 *   </ul>
 *
 * <p>Consider using {@code Message}-based IDs if you want to have typed IDs in your code, and/or
 * if you need to have IDs with some structure inside. Examples of such structural IDs are:
 *   <ul>
 *      <li>EAN value used in bar codes</li>
 *      <li>ISBN</li>
 *      <li>Phone number</li>
 *      <li>email address as a couple of local-part and domain</li>
 *   </ul>
 *
 * @author Alexander Yevsyukov
 */
public final class AggregateId<I> {

    private final I value;

    private AggregateId(I value) {
        this.value = checkNotNull(value);
    }

    /**
     * Creates a new non-null id of an aggregate root.
     *
     * @param value id value
     * @return new instance
     */
    public static <I> AggregateId<I> of(I value) {
        return new AggregateId<>(value);
    }

    @SuppressWarnings("TypeMayBeWeakened") // We want already built instances at this level of API.
    public static AggregateId<? extends Message> of(EventContext value) {
        final Message message = Messages.fromAny(value.getAggregateId());
        final AggregateId<Message> result = new AggregateId<>(message);
        return result;
    }

    /**
     * Returns the short name of the type of underlying value.
     *
     * @return
     *  <ul>
     *      <li>Short Protobuf type name if the value is {@link Message}</li>
     *      <li>Simple class name of the value, otherwise</li>
     *  </ul>
     */
    public String getShortTypeName() {
        if (this.value instanceof Message) {
            //noinspection TypeMayBeWeakened
            Message message = (Message)this.value;
            Descriptors.Descriptor descriptor = message.getDescriptorForType();
            final String result = descriptor.getName();
            return result;
        } else {
            String result = value.getClass().getSimpleName();
            return result;
        }
    }

    @Override
    public String toString() {
        final String result = Entity.idToString(value());
        return result;
    }

    public I value() {
        return this.value;
    }
}
