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
package org.spine3.client;

import org.spine3.time.ZoneOffset;

/**
 * @author Alex Tymchenko
 */
public class TopicFactory extends ActorRequestFactory<TopicFactory> {

    protected TopicFactory(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates new factory with the same user and tenant ID, but with new time zone offset.
     *
     * @param zoneOffset the offset of the time zone
     * @return new query factory at new time zone
     */
    @Override
    public TopicFactory switchTimezone(ZoneOffset zoneOffset) {
        return switchTimezone(zoneOffset, newBuilder());
    }

    public static class Builder
            extends ActorRequestFactory.AbstractBuilder<TopicFactory, TopicFactory.Builder> {

        @Override
        protected TopicFactory.Builder thisInstance() {
            return this;
        }

        @Override
        public TopicFactory build() {
            super.build();
            final TopicFactory result = new TopicFactory(this);
            return result;
        }
    }
}
