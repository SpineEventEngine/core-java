/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server;

import com.google.protobuf.Any;
import io.spine.core.Version;
import io.spine.server.model.Didnt;

/**
 * An object with identity which produces events.
 *
 * @author Alexander Yevsyukov
 */
public interface EventProducer {

    /**
     * The object identity packed into {@link Any}.
     */
    Any getProducerId();

    /**
     * The version of the object to be put into events.
     *
     * <p>If {@linkplain Version#getDefaultInstance() empty}, no version will be added to
     * the generated events.
     */
    Version getVersion();

    /**
     * Obtains the {@link Didnt} event message.
     *
     * <p>This event should be returned if there is no value for the domain to produce an actual
     * event. Note that a {@link Didnt} event is never actually posted into
     * the {@link io.spine.server.event.EventBus EventBus}.
     *
     * @return the default instance of {@link Didnt}
     */
    default Didnt nothing() {
        return Didnt.getDefaultInstance();
    }
}
