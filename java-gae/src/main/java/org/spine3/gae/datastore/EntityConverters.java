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

package org.spine3.gae.datastore;

import com.google.appengine.api.datastore.Entity;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.util.ClassName;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds Entity Converters and provides an API for them.
 *
 * @author Mikhayil Mikhaylov
 */
class EntityConverters {

    private static ConvertersMap convertersMap = new ConvertersMap();
    static {
        convertersMap.put(new CommandRequestConverter());
        convertersMap.put(new EventRecordConverter());
    }

    public static Entity convert(Message message) {
        final Class messageClass = message.getClass();

        return convertersMap.get(messageClass).convert(message);
    }

    private static class ConvertersMap {
        private Map<Class, Converter> converters = new HashMap<>();

        public void put(Converter converter) {
            converters.put(converter.getMessageClass(), converter);
        }

        public Converter get(Class clazz) {
            if (!converters.containsKey(clazz)) {
                throw new IllegalArgumentException("Unsupported message class: " + clazz.getSimpleName());
            }
            return converters.get(clazz);
        }
    }
}