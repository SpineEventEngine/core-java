/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.entity;

import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.server.event.EventDispatcher;
import org.spine3.server.type.EventClass;

/**
 * Delivers events to handlers (which are supposed to be entities).
 *
 * @param <I> the type of entity IDs
 * @author Alexander Litus
 * @see EventDispatcher
 */
public interface EntityEventDispatcher<I> extends EventDispatcher {

    //TODO:2017-01-04:alexander.yevsyukov: This should return IdSetFunction instead.
    //TODO:2017-01-04:alexander.yevsyukov: Then make ProjectionRepository implement EntityEventDispatcher
    /**
     * Returns a function which can obtain an ID using a message of the passed class.
     *
     * @param eventClass a class of any event handled by the entity
     * @return an ID function
     */
    IdFunction<I, ? extends Message, EventContext> getIdFunction(EventClass eventClass);
}
