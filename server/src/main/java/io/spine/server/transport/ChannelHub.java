/*
 * Copyright 2019, TeamDev. All rights reserved.
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
package io.spine.server.transport;

import com.google.common.collect.ImmutableSet;
import io.spine.annotation.SPI;
import io.spine.type.TypeUrl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.Collections.synchronizedMap;

/**
 * The hub of channels grouped in some logical way.
 *
 * <p>Serves for channel creation and storage-per-ID which in a way makes the hub similar to
 * an entity repository.
 */
@SPI
public abstract class ChannelHub<C extends MessageChannel> implements AutoCloseable {

    private final TransportFactory transportFactory;
    private final Map<TypeUrl, C> channels = synchronizedMap(new HashMap<>());

    protected ChannelHub(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    /**
     * Creates a new channel under the specified ID.
     *
     * @param targetType
     *         type of the messages transmitted in the resulting channel
     * @return the created channel.
     */
    protected abstract C newChannel(TypeUrl targetType);

    public synchronized ImmutableSet<TypeUrl> types() {
        return ImmutableSet.copyOf(channels.keySet());
    }


    /**
     * Obtains a channel from this hub according to the channel ID.
     *
     * <p>If there is no channel with this ID in this hub, creates it and adds to the hub
     * prior to returning it as a result of this method call.
     *
     * @param targetType the message
     * @return a channel with the key
     */
    public synchronized C get(TypeUrl targetType) {
        C channel = channels.computeIfAbsent(targetType, this::newChannel);
        return channel;
    }

    /**
     * Closes the stale channels and removes those from the hub.
     */
    public void closeStaleChannels() {
        Set<TypeUrl> staleChannels = detectStale();
        for (TypeUrl id : staleChannels) {
            channels.remove(id);
        }
    }

    private Set<TypeUrl> detectStale() {
        Set<TypeUrl> toRemove = newHashSet();
        for (TypeUrl type : channels.keySet()) {
            C channel = channels.get(type);
            if (channel.isStale()) {
                try {
                    channel.close();
                } catch (Exception e) {
                    throw illegalStateWithCauseOf(e);
                } finally {
                    toRemove.add(type);
                }
            }
        }
        return toRemove;
    }

    @Override
    public void close() throws Exception {
        for (C channel : channels.values()) {
            channel.close();
        }
        channels.clear();
    }

    TransportFactory transportFactory() {
        return transportFactory;
    }
}
