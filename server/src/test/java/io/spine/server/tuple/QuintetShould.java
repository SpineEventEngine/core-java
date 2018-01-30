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

package io.spine.server.tuple;

import com.google.common.base.Optional;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.test.tuple.quintet.InstrumentNumber;
import io.spine.test.tuple.quintet.Viola;
import io.spine.test.tuple.quintet.Violin;
import io.spine.test.tuple.quintet.ViolinCello;
import io.spine.validate.Validate;
import org.junit.Test;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static io.spine.server.tuple.QuintetShould.InstrumentFactory.newViola;
import static io.spine.server.tuple.QuintetShould.InstrumentFactory.newViolin;
import static io.spine.server.tuple.QuintetShould.InstrumentFactory.newViolinCello;
import static io.spine.server.tuple.QuintetShould.QuintetFactory.NUM_1;
import static io.spine.server.tuple.QuintetShould.QuintetFactory.NUM_2;
import static io.spine.server.tuple.QuintetShould.QuintetFactory.newCelloQuintet;
import static io.spine.server.tuple.QuintetShould.QuintetFactory.newViolaQuintet;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link Quintet} tuple.
 *
 * <p>This test suite uses <a href="https://en.wikipedia.org/wiki/String_quintet">String quintets</a>
 * types just to have test data other than default Protobuf types, and for some fun.
 *
 * <p>In a real app a {@link Quintet} should have only event messages, and not value objects.
 *
 * @author Alexander Yevsyukov
 */
public class QuintetShould {

    private final Quintet celloQuintet = newCelloQuintet();
    private final Quintet violaQuintet = newViolaQuintet();

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester().setDefault(Message.class, newViola())
                               .setDefault(Optional.class, Optional.of(newViolinCello()))
                               .testAllPublicStaticMethods(Quintet.class);
    }

    @Test
    public void serialize() {
        reserializeAndAssert(celloQuintet);
        reserializeAndAssert(violaQuintet);

        reserializeAndAssert(
                Quintet.withNullable(newViola(), newViola(), newViola(), newViola(), null)
        );

        reserializeAndAssert(
                Quintet.withNullable2(newViola(), newViola(), newViola(), null, null)
        );

        reserializeAndAssert(
                Quintet.withNullable3(newViola(), newViola(), null, null, null)
        );

        reserializeAndAssert(
                Quintet.withNullable4(newViola(), null, null, null, null)
        );
    }

    @Test
    public void support_equality() {
        new EqualsTester().addEqualityGroup(newCelloQuintet(), newCelloQuintet())
                          .addEqualityGroup(newViolaQuintet())
                          .testEquals();
    }

    @Test
    public void return_elements() {
        assertEquals(newViolin(NUM_1), celloQuintet.getA());
        assertEquals(newViolin(NUM_2), celloQuintet.getB());
        assertEquals(newViola(), celloQuintet.getC());
        assertEquals(newViolinCello(NUM_1), celloQuintet.getD());
        assertEquals(newViolinCello(NUM_2), celloQuintet.getE());
    }

    /*
     * Test environment
     *********************/

    /**
     * Creates typical <a href="https://en.wikipedia.org/wiki/String_quintet">String quintets</a>.
     */
    static class QuintetFactory {

        static final InstrumentNumber NUM_1 = InstrumentNumber.newBuilder()
                                                              .setValue(1)
                                                              .build();

        static final InstrumentNumber NUM_2 = InstrumentNumber.newBuilder()
                                                              .setValue(2)
                                                              .build();

        /** Prevents instantiation of this utility class. */
        private QuintetFactory() {
        }

        static Quintet<Violin, Violin, Viola, ViolinCello, ViolinCello> newCelloQuintet() {
            return Quintet.of(newViolin(NUM_1),
                              newViolin(NUM_2),
                              newViola(),
                              newViolinCello(NUM_1),
                              newViolinCello(NUM_2));
        }

        static Quintet<Violin, Violin, Viola, Viola, ViolinCello> newViolaQuintet() {
            return Quintet.of(newViolin(NUM_1),
                              newViolin(NUM_2),
                              newViola(NUM_1),
                              newViola(NUM_2),
                              newViolinCello());
        }
    }

    /**
     * Creates instruments.
     */
    static class InstrumentFactory {

        /** Prevents instantiation of this utility class. */
        private InstrumentFactory() {
        }

        static Violin newViolin(InstrumentNumber number) {
            final Violin result = Violin.newBuilder()
                                        .setNumber(number)
                                        .build();
            Validate.checkValid(result);
            return result;
        }

        static Viola newViola() {
            final Viola result = Viola.newBuilder()
                                      .setSingle(true)
                                      .build();
            Validate.checkValid(result);
            return result;
        }

        static Viola newViola(InstrumentNumber number) {
            final Viola result = Viola.newBuilder()
                                      .setNumber(number)
                                      .build();
            Validate.checkValid(result);
            return result;
        }

        static ViolinCello newViolinCello(InstrumentNumber number) {
            final ViolinCello result = ViolinCello.newBuilder()
                                                  .setNumber(number)
                                                  .build();
            Validate.checkValid(result);
            return result;
        }

        static ViolinCello newViolinCello() {
            final ViolinCello result = ViolinCello.newBuilder()
                                                  .setSingle(true)
                                                  .build();
            Validate.checkValid(result);
            return result;
        }
    }
}
