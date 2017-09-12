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
package io.spine.server.integration;

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.type.MessageClass;

/**
 * @author Alex Tymchenko
 */
@Internal
public class ExternalMessageClass extends MessageClass {

    private static final long serialVersionUID = 0L;

    private ExternalMessageClass(Class<? extends Message> value) {
        super(value);
    }

    static ExternalMessageClass of(ExternalMessage message) {
        //TODO:2017-07-24:alex.tymchenko: implement properly
        final MessageClass messageClass = new MessageClass(message.getClass()) {};
        return of(messageClass);
    }

    public static ExternalMessageClass of(MessageClass messageClass) {
        return new ExternalMessageClass(messageClass.value());
    }

    static ExternalMessageClass of(Class<? extends Message> clz) {
        return new ExternalMessageClass(clz);
    }
}
