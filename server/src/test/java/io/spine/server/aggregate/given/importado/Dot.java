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

package io.spine.server.aggregate.given.importado;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.aggregate.given.importado.command.Move;
import io.spine.server.aggregate.given.importado.event.Moved;
import io.spine.server.command.Assign;

import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * An object moving in a 2-D space.
 *
 * @author Alexander Yevsyukov
 */
public class Dot extends Aggregate<ObjectId, Point, PointVBuilder> {

    protected Dot(ObjectId id) {
        super(id);
    }

    @Assign
    Moved on(Move command) {
        return Moved.newBuilder()
                    .setObject(command.getObject())
                    .setDirection(command.getDirection())
                    .setCurrentPosition(move(getState(), command.getDirection()))
                    .build();
    }

    @Apply(allowImport = true)
    void event(Moved event) {
        Point newPosition = move(getState(), event.getDirection());
        getBuilder().setX(newPosition.getX())
                    .setY(newPosition.getY());
    }

    private static Point move(Point p, Direction direction) {
        PointVBuilder result = PointVBuilder
                .newBuilder()
                .setX(p.getX())
                .setY(p.getY());
        switch (direction) {
            case NORTH:
                result.setY(p.getY() + 1);
                break;
            case EAST:
                result.setX(p.getX() + 1);
                break;
            case SOUTH:
                result.setY(p.getY() - 1);
                break;
            case WEST:
                result.setX(p.getX() - 1);
                break;
            default:
                throw newIllegalArgumentException("Invalid direction passed %s", direction);
        }
        return result.build();
    }
}
