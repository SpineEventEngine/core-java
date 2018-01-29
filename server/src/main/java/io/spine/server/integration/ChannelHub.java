/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import io.spine.annotation.SPI;
import io.spine.type.MessageClass;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.Collections.synchronizedMap;

/**
 * The hub of channels, grouped in some logical way.
 *
 * <p>Serves for channel creation and storage-per-key, which in a way makes the hub similar to
 * an entity repository.
 *
 * @author Alex Tymchenko
 */
@SPI
public abstract class ChannelHub<C extends MessageChannel> implements AutoCloseable {

    private final TransportFactory transportFactory;
    private final Map<ExternalMessageClass, C> channels =
            synchronizedMap(Maps.<ExternalMessageClass, C>newHashMap());

    protected ChannelHub(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    /**
     * Creates a new channel under the specified key
     *
     * @param channelKey the channel key to use
     * @return the created channel.
     */
    protected abstract C newChannel(MessageClass channelKey);

    public synchronized Set<ExternalMessageClass> keys() {
        return ImmutableSet.copyOf(channels.keySet());
    }

    /**
     * Obtains a channel from this hub according to the channel key.
     *
     * <p>If there is no channel with this key in this hub, creates it and adds to the hub
     * prior to returning it as a result of this method call.
     *
     * @param channelKey the channel key to obtain a channel with
     * @return a channel with the key
     */
    public synchronized C get(MessageClass channelKey) {
        final ExternalMessageClass key = ExternalMessageClass.of(channelKey);
        if(!channels.containsKey(key)) {
            final C newChannel = newChannel(key);
            channels.put(key, newChannel);
        }
        return channels.get(key);
    }

    /**
     * Closes the stale channels and removes those from the hub.
     */
    public void closeStaleChannels() {
        final Set<ExternalMessageClass> staleChannels = detectStale();
        for (ExternalMessageClass cls : staleChannels) {
            channels.remove(cls);
        }
    }

    private Set<ExternalMessageClass> detectStale() {
        final Set<ExternalMessageClass> toRemove = newHashSet();
        for (ExternalMessageClass cls : channels.keySet()) {
            final C channel = channels.get(cls);
            if(channel.isStale()) {
                try {
                    channel.close();
                } catch (Exception e) {
                    throw illegalStateWithCauseOf(e);
                } finally {
                    toRemove.add(cls);
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
