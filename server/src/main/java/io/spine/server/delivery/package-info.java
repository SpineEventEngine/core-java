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

/**
 * This package is devoted to message delivery and sharding.
 *
 * <h2>Preface</h2>
 *
 * <p>Very often Spine-based applications are designed for Google Cloud deployment. In particular,
 * an AppEngine automatic scaling feature is heavily utilized in order to achieve the best
 * performance/cost ratio.
 *
 * <p>While such an approach requires zero maintenance of the computational nodes, some special
 * attention must be paid to the concurrent access to the data. The thing is that AppEngine
 * by default performs round-robin routing of the requests sent to nodes. So that write-side nodes
 * may be updating the same {@code Aggregate} instances at the same moment.
 *
 * <p>Similar issues occur when studying the event delivery from the write-side to read-side.
 * The same instances of {@code Projection}s must be updated in a synchronous and exclusive fashion.
 * Otherwise their state is going to be broken.
 *
 *
 * <h2>Sharding</h2>
 *
 * <p>{@code Aggregate}s, {@code ProcessManager}s and {@code Projection}s are grouped into shards.
 *
 * <p>A shard is virtual group of entities of the same entity class (e.g. a group of
 * certain {@code Projection} instances). In scope of a shard all the entities should
 * be processed synchronously, in a single thread per shard.
 *
 * <p>So if an {@code Aggregate} declares commands to handle, events and rejections to react on,
 * they should all be processed synchronously for a certain {@code Aggregate} instance.
 * The events produced in each command handler and event/rejection reactors
 * should be applied in the same thread as well. Such a synchronous entity processing allows to
 * prevent the concurrent modification of the entity state.
 *
 * <p>The delivery strategies of each of the entity repositories are used to reroute and regroup
 * the messages sent for dispatching, and then dispatch those to the instances per-shard.
 *
 *
 * <h3>Flow</h3>
 *
 * <i>(illustrated for Commands, however Events and Rejections are dispatched similarly)</i>
 *
 * <p><img src="./doc-files/sharding-commands-flow.png" alt="Sharding command flow">
 *
 * <p>In this flow the message channel serves as a single-lane road, enqueuing the messages
 * from multiple senders (potentially, posting their messages simultaneously) and transferring
 * them to a single window on a receiving side.
 *
 * <h3>Transport</h3>
 *
 * <p>A {@linkplain io.spine.server.delivery.ShardedStream ShardedStream} is introduced as
 * a convenience wrapper over channels, used to exchange the messages to each of the shards.
 *
 *
 * <h3>Configuration</h3>
 *
 * <p>By default each repository defines a number of shards equal to {@code 1}, so all the entities
 * of the repository belong to a single shard.
 *
 * <p>To define a custom sharding strategy for the entity repository one should override the
 * {@link io.spine.server.delivery.Shardable#getShardingStrategy() getShardingStrategy()}.
 * One of the possible values to return is a
 * {@link io.spine.server.delivery.UniformAcrossTargets UniformAcrossTargets}-produced value:
 *
 * <pre>
 *  {@code
 *  public static class TripleShardProjectRepository
 *             extends AggregateRepository&lt;ProjectId, DeliveryProject&gt; {
 *
 *         public TripleShardProjectRepository() {
 *             super();
 *             getRejectionRouting().replaceDefault(routeByProjectId());
 *         }
 *
 *         {@literal @}Override
 *         public ShardingStrategy getShardingStrategy() {
 *             return UniformAcrossTargets.forNumber(3);
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>In the example above the entities will be split into three shards by a their
 * {@code getId().hashCode() % 3} value. While such an approach is hard to call a truly uniform,
 * as the nature of identifiers is completely different from domain to domain, it seems to be good
 * enough for many typical cases.
 *
 * <p>However, it's <b>extremely</b> important to remember that the transport implementation
 * means a lot. The provided out-of-the-box in-process implementation is naïve and should be
 * replaced with something playing well with the underlying infrastructure.
 *
 *
 * <h3>Implementation Details</h3>
 *
 * <p>A JVM-wide {@link io.spine.server.delivery.Sharding Sharding} service is introduced. Its
 * implementation is exposed via {@link io.spine.server.ServerEnvironment ServerEnvironment} and is
 * used to integrate the transport factory, register new shards and define which shards
 * are served within the current JVM.
 *
 * <p>{@code AggregateRepository}, {@code ProcessManagerRepository} and {@code ProjectionRepository}
 * become shardable by implementing the {@link io.spine.server.delivery.Shardable} interface.
 *
 * <p>Each {@code Shardable} defines the total number of shards and the way to tell the shard
 * by the entity ID.
 *
 * <p>Each {@code Shardable} defines a number of message consumers, each devoted to consuming
 * messages of a certain kind (e.g. {@code Command}). A special
 * {@link io.spine.server.delivery.ShardedStreamConsumer ShardedStreamConsumer} interface is
 * introduced for this matter. The consumers are used to receive the messages, sent to a specific
 * shard. To specify the type of messages and the type of the entity to which messages are headed,
 * a {@link io.spine.server.delivery.DeliveryTag DeliveryTag} must be defined for the each consumer.
 *
 * <p>The {@code Delivery} is split into {@linkplain io.spine.server.delivery.Sender sending} and
 * {@linkplain io.spine.server.delivery.Consumer consuming} parts, each communicating with
 * the transport layer. The consuming part of a {@code Delivery} is a {@code ShardedStreamConsumer}
 * implementation.
 *
 * <p>An internal {@link io.spine.server.delivery.ShardingRegistry ShardingRegistry} is used
 * to hold all the known consumers and their streams.  Its instance belongs to the {@code Sharding}
 * service and is JVM-wide as well.
 */
@ParametersAreNonnullByDefault
package io.spine.server.delivery;

import javax.annotation.ParametersAreNonnullByDefault;
