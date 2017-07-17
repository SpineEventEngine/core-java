package io.spine.server.route;

import com.google.protobuf.Message;

import java.util.Set;

/**
 * A route for a message to be delivered to several entities.
 *
 * @param <I> the type of the entity IDs
 * @author Alexander Yevsyukov
 */
public interface Multicast<I, M extends Message, C extends Message> extends Route<M, C, Set<I>>  {
}
