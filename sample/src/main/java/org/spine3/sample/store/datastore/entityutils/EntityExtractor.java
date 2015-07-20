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

package org.spine3.sample.store.datastore.entityutils;

import com.google.appengine.api.datastore.Entity;
import com.google.protobuf.Message;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;

import java.util.HashMap;

/**
 * Extracts entities from Message classes by appropriate Any.getTypeUrl().
 *
 * @author Mikhayil Mikhaylov
 */
public class EntityExtractor {

    private static HashMap<String, TypedEntityExtractor> extractors = new HashMap<String, TypedEntityExtractor>() {{
        put(EventRecord.getDescriptor().getFullName(), new EventRecordEntityExtractor());
        put(CommandRequest.getDescriptor().getFullName(), new CommandRequestEntityExtractor());
    }};

    public static Entity extract(Message message, String typeUrl) {
        if (!extractors.containsKey(typeUrl)) {
            throw new IllegalArgumentException("Unknown message type");
        }

        return extractors.get(typeUrl).extract(message);
    }

}