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

package org.spine3.gae.datastore.entityutils;

import com.google.appengine.api.datastore.Entity;
import com.google.protobuf.Message;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;

import java.util.HashMap;

/**
 * Holds Entity Converters and provides an API for them.
 *
 * @author Mikhayil Mikhaylov
 */
public class EntityConverters {

    private static HashMap<ClassName, EntityConverter> converters = new HashMap<ClassName, EntityConverter>() {{
        put(ClassName.of(EventRecord.class), new EventRecordEntityConverter());
        put(ClassName.of(CommandRequest.class), new CommandRequestEntityConverter());
    }};

    public static Entity convert(Message message) {
        final ClassName messageClassName = ClassName.of(message.getClass());
        if (!converters.containsKey(messageClassName)) {
            throw new IllegalArgumentException("Unknown message type");
        }

        return converters.get(messageClassName).convert(message);
    }

}