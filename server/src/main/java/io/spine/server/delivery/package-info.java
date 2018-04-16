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
 * Otherwise their state is going to be totally broken.
 *
 *
 * <h2>Sharding</h2>
 *
 * <p>{@code Aggregate}s, {@code ProcessManager}s and {@code Projection}s are grouped into shards.
 *
 * <p>A shard is virtual group of entities of the same kind. In scope of a shard the entities should
 * be processed synchronously (e.g. in a single thread). For instance, the commands to handle,
 * events and rejections to react on are all processed synchronously for the same instance
 * of an {@code Aggregate} — as each of them relies on the same entity state. More than that,
 * the events produced in each command handler and event/rejection reactors are applied
 * in the same thread — to prevent the concurrent modifications of the entity.
 *
 * <p>The delivery strategies of each of the entity repositories are used to reroute and regroup
 * the messages sent for dispatching, and then dispatch those to the instances per-shard.
 *
 *
 * <h3>Flow</h3>
 *
 * <i>How a  command is handled by some aggregate instance.</i>
 *
 * <p>End-user  --> the command is emitted --> gRPC `CommandService` --> `BoundedContext` selected
 * --> the command is posted to its command bus --> `AggregateRepository` instance receives
 * the command --> `AggregateCommandEndpoint` is given the command to dispatch it
 * to the `Aggregate` instance ...
 *
 * <ul>
 *      <li> --> The endpoint {@code Delivery} detects the target shard by the aggregate ID.</li>
 *      <li> --> The command is packed into a transferable package and sent to the (remote)
 *          channel corresponding to the shard. The message channel consumers are also endpoint
 *          {@code Delivery} instances, each serving its own shard.</li>
 *
 *      <li> --> The command is received by the {@code Delivery}'s consuming part, unpacked
 *      and dispatched to the proper {@code Aggregate} instance.</li>
 * </ul>
 *
 *
 * <p>In this flow the message channel serves as a single-lane road, enqueuing the messages
 * from multiple senders (potentially, posting their messages simultaneously) and transferring
 * them to a single window on a receiving side.
 *
 * <h3>Transport</h3>
 *
 * <p>The Spine transport routines are enhanced in this PR to meet the needs of sharding.
 *
 * <ul>
 *      <li>Channels are now identified by a generic {@code ChannelId} instead of a
 *      channel-per-message-type strategy as previously.</li>
 *      <li>In-memory implementation transport made more concurrency-friendly.</li>
 * </ul>
 *
 * <p>In addition, a {@linkplain io.spine.server.delivery.ShardedStream ShardedStream} has been
 * introduced as a convenience wrapper over channels, used to exchange the messages
 * to each of the shards.
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
 * <p>Spine GAE-related library will be releasing the implementation based upon Google PubSub soon.
 *
 *
 * <h3>Implementation Details</h3>
 *
 * <p>A JVM-wide {@link io.spine.server.delivery.Sharding Sharding} service is introduced. Its
 * implementation is exposed via {@link io.spine.server.ServerEnvironment ServerEnvironment} and is
 * used to glue up the transport factory, register new shards and, most importantly,
 * define which shards are served within the current JVM.
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
